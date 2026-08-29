package com.DBArena.services.execution.service;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.ExecutionError;
import com.DBArena.engine.spi.model.ExplainPlan;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.domain.Execution;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.domain.ExecutionStatus;
import com.DBArena.services.execution.executor.QueryExecutionOutcome;
import com.DBArena.services.execution.executor.QueryExecutor;
import com.DBArena.services.execution.explain.ExplainProvider;
import com.DBArena.services.execution.repository.ExecutionRepository;
import com.DBArena.services.execution.sandbox.SandboxProvider;
import com.DBArena.services.execution.validation.QueryValidatorRegistry;
import com.DBArena.services.execution.validation.ValidationResult;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The whole request flow in one place: request -&gt; validation -&gt; policy
 * -&gt; sandbox -&gt; execution -&gt; metrics -&gt; result (the task brief's own
 * words). Validation is synchronous (fast; the caller finds out
 * immediately whether their statement was rejected); everything from
 * sandbox acquisition onward runs on {@code executionWorkerPool} so
 * {@code POST /api/v1/executions} returns as soon as a statement is
 * accepted and queued, not when it finishes.
 */
@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final QueryValidatorRegistry queryValidatorRegistry;
    private final SandboxProvider sandboxProvider;
    private final QueryExecutor queryExecutor;
    private final ExplainProvider explainProvider;
    private final ExecutionAuditLogger auditLogger;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ExecutionPolicy policy;
    private final ExecutorService workerPool;
    private final Semaphore globalConcurrencySemaphore;

    /**
     * Per-user in-flight counters and cancellation flags, in-memory only -
     * same "single-instance-scoped, deliberately" posture ai-assistant-
     * service's {@code HintRateLimiter} already documents for this exact
     * kind of state; swap for a shared store before this service ever runs
     * as more than one instance.
     */
    private final ConcurrentHashMap<String, AtomicInteger> inFlightByUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<?>> runningFutures = new ConcurrentHashMap<>();
    private final Set<String> cancelledIds = ConcurrentHashMap.newKeySet();

    public ExecutionService(
            ExecutionRepository executionRepository,
            QueryValidatorRegistry queryValidatorRegistry,
            SandboxProvider sandboxProvider,
            QueryExecutor queryExecutor,
            ExplainProvider explainProvider,
            ExecutionAuditLogger auditLogger,
            IdGenerator idGenerator,
            Clock clock,
            ExecutionPolicy policy,
            ExecutorService executionWorkerPool,
            Semaphore globalConcurrencySemaphore) {
        this.executionRepository = executionRepository;
        this.queryValidatorRegistry = queryValidatorRegistry;
        this.sandboxProvider = sandboxProvider;
        this.queryExecutor = queryExecutor;
        this.explainProvider = explainProvider;
        this.auditLogger = auditLogger;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.policy = policy;
        this.workerPool = executionWorkerPool;
        this.globalConcurrencySemaphore = globalConcurrencySemaphore;
    }

    public Execution create(
            AuthenticatedUser user, EngineType engine, String datasetSlug, Optional<String> problemSlug, String statementText) {
        String userKey = user.userId().value();
        acquirePerUserSlot(userKey);

        long now = clock.millis();
        Execution execution = Execution.requested(idGenerator.nextTyped(), user.userId(), engine, datasetSlug, problemSlug, statementText, now);
        executionRepository.insert(execution);
        auditLogger.log(execution, "REQUESTED");

        execution = execution.withStatus(ExecutionStatus.VALIDATING);
        executionRepository.replace(execution);

        ValidationResult validation = queryValidatorRegistry.resolve(engine).validate(statementText, policy);
        if (!validation.allowed()) {
            execution = execution.withRejected(validation.rejectionReason().orElse("rejected"), clock.millis());
            executionRepository.replace(execution);
            auditLogger.log(execution, "REJECTED: " + validation.rejectionReason().orElse(""));
            releasePerUserSlot(userKey);
            return execution;
        }

        execution = execution.withStatus(ExecutionStatus.QUEUED);
        executionRepository.replace(execution);
        auditLogger.log(execution, "QUEUED");

        TypedId<Execution> id = execution.id();
        Future<?> future = workerPool.submit(() -> runAsync(id, userKey, engine, datasetSlug, statementText));
        runningFutures.put(id.value(), future);
        return execution;
    }

    public Execution getOwned(AuthenticatedUser user, TypedId<Execution> id) {
        Execution execution = executionRepository.findById(id).orElseThrow(() -> new ExecutionNotFoundException(id.value()));
        if (!execution.userId().equals(user.userId())) {
            throw new ExecutionNotFoundException(id.value()); // see ExecutionNotFoundException's Javadoc: never a distinguishable 403
        }
        return execution;
    }

    public CursorPage<Execution> listOwned(AuthenticatedUser user, PageRequest pageRequest) {
        return executionRepository.findPageByUserId(user.userId(), pageRequest);
    }

    public Execution cancel(AuthenticatedUser user, TypedId<Execution> id) {
        Execution execution = getOwned(user, id);
        if (execution.status().isTerminal()) {
            return execution; // idempotent - cancelling an already-finished execution is a no-op, not an error
        }
        cancelledIds.add(id.value());
        Future<?> future = runningFutures.get(id.value());
        if (future != null) {
            future.cancel(true); // best-effort - see SandboxProvider/StatementRequest's own timeout as the hard backstop
        }
        Execution current = executionRepository.findById(id).orElseThrow(() -> new ExecutionNotFoundException(id.value()));
        if (!current.status().isTerminal()) {
            current = current.withCancelled(clock.millis());
            executionRepository.replace(current);
            auditLogger.log(current, "CANCELLED");
        }
        return current;
    }

    public ExplainPlan explain(AuthenticatedUser user, TypedId<Execution> id) {
        Execution execution = getOwned(user, id);
        ValidationResult validation = queryValidatorRegistry.resolve(execution.engine()).validate(execution.statementText(), policy);
        if (!validation.allowed()) {
            throw new com.DBArena.common.core.error.ValidationException(java.util.List.of(
                    new com.DBArena.common.core.error.FieldViolation(
                            "statementText", validation.rejectionReason().orElse("statement rejected"))));
        }
        SessionHandle session = sandboxProvider.acquire(execution.engine(), execution.datasetSlug(), policy);
        try {
            return explainProvider.explain(session, execution.engine(), execution.statementText(), policy);
        } finally {
            sandboxProvider.release(session);
        }
    }

    private void runAsync(TypedId<Execution> id, String userKey, EngineType engine, String datasetSlug, String statementText) {
        boolean acquiredGlobalPermit = false;
        SessionHandle session = null;
        try {
            if (isCancelled(id)) {
                finishCancelled(id);
                return;
            }
            acquiredGlobalPermit = globalConcurrencySemaphore.tryAcquire(policy.statementTimeout().toSeconds(), TimeUnit.SECONDS);
            if (!acquiredGlobalPermit) {
                finishResourceLimit(id, "global concurrent execution limit reached - try again shortly");
                return;
            }

            transitionTo(id, ExecutionStatus.STARTING, true);
            session = sandboxProvider.acquire(engine, datasetSlug, policy);

            if (isCancelled(id)) {
                finishCancelled(id);
                return;
            }
            transitionTo(id, ExecutionStatus.EXECUTING, false);
            QueryExecutionOutcome outcome = queryExecutor.execute(session, engine, statementText, policy);

            if (isCancelled(id)) {
                finishCancelled(id);
                return;
            }
            transitionTo(id, ExecutionStatus.EVALUATING, false);

            if (outcome.isSuccess()) {
                finishCompleted(id, outcome);
            } else {
                ExecutionError error = outcome.raw().error().orElseThrow();
                if (isTimeoutError(error)) {
                    finishTimeout(id, error.message());
                } else {
                    finishEngineError(id, error.message());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finishCancelled(id);
        } catch (RuntimeException e) {
            finishEngineError(id, "internal error while executing this statement");
        } finally {
            if (session != null) {
                sandboxProvider.release(session);
            }
            if (acquiredGlobalPermit) {
                globalConcurrencySemaphore.release();
            }
            releasePerUserSlot(userKey);
            runningFutures.remove(id.value());
            cancelledIds.remove(id.value());
        }
    }

    private void acquirePerUserSlot(String userKey) {
        AtomicInteger counter = inFlightByUser.computeIfAbsent(userKey, k -> new AtomicInteger());
        int updated = counter.incrementAndGet();
        if (updated > policy.maxConcurrentPerUser()) {
            counter.decrementAndGet();
            throw new TooManyConcurrentExecutionsException("per-user");
        }
    }

    private void releasePerUserSlot(String userKey) {
        inFlightByUser.computeIfPresent(userKey, (k, counter) -> {
            counter.decrementAndGet();
            return counter;
        });
    }

    private boolean isCancelled(TypedId<Execution> id) {
        return cancelledIds.contains(id.value());
    }

    private void transitionTo(TypedId<Execution> id, ExecutionStatus status, boolean stampStarted) {
        Execution execution = executionRepository.findById(id).orElseThrow(() -> new ExecutionNotFoundException(id.value()));
        Execution updated = stampStarted ? execution.withStarted(clock.millis()) : execution.withStatus(status);
        executionRepository.replace(updated);
        auditLogger.log(updated, status.name());
    }

    private void finishCompleted(TypedId<Execution> id, QueryExecutionOutcome outcome) {
        Execution execution = executionRepository.findById(id).orElseThrow(() -> new ExecutionNotFoundException(id.value()));
        var evaluation = outcome.evaluation().orElseThrow();
        Execution updated = execution.withCompleted(evaluation.summary(), evaluation.metrics(), clock.millis());
        executionRepository.replace(updated);
        auditLogger.log(updated, "COMPLETED");
    }

    private void finishTimeout(TypedId<Execution> id, String message) {
        finishFailure(id, ExecutionStatus.TIMEOUT, message);
    }

    private void finishEngineError(TypedId<Execution> id, String message) {
        finishFailure(id, ExecutionStatus.ENGINE_ERROR, message);
    }

    private void finishResourceLimit(TypedId<Execution> id, String message) {
        finishFailure(id, ExecutionStatus.RESOURCE_LIMIT, message);
    }

    private void finishCancelled(TypedId<Execution> id) {
        Execution execution = executionRepository.findById(id).orElseThrow(() -> new ExecutionNotFoundException(id.value()));
        if (execution.status().isTerminal()) {
            return;
        }
        Execution updated = execution.withCancelled(clock.millis());
        executionRepository.replace(updated);
        auditLogger.log(updated, "CANCELLED");
    }

    private void finishFailure(TypedId<Execution> id, ExecutionStatus status, String message) {
        Execution execution = executionRepository.findById(id).orElseThrow(() -> new ExecutionNotFoundException(id.value()));
        Execution updated = execution.withFailure(status, message, clock.millis());
        executionRepository.replace(updated);
        auditLogger.log(updated, status.name());
    }

    /** Postgres's SQLSTATE 57014 ("query_canceled") is what a statement-timeout cancellation surfaces as - see PostgresEngineAdapter's own error-code convention ("postgres." + SQLState). */
    private static boolean isTimeoutError(ExecutionError error) {
        return "postgres.57014".equals(error.code());
    }
}
