package com.DBArena.services.execution.service;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.ColumnMeta;
import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.domain.Execution;
import com.DBArena.services.execution.domain.ExecutionMetrics;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.domain.ExecutionResultSummary;
import com.DBArena.services.execution.domain.ExecutionStatus;
import com.DBArena.services.execution.executor.QueryExecutionOutcome;
import com.DBArena.services.execution.executor.QueryExecutor;
import com.DBArena.services.execution.explain.ExplainProvider;
import com.DBArena.services.execution.repository.ExecutionRepository;
import com.DBArena.services.execution.sandbox.SandboxProvider;
import com.DBArena.services.execution.validation.QueryValidatorRegistry;
import com.DBArena.services.execution.validation.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExecutionServiceTest {

    private ExecutionRepository repository;
    private QueryValidatorRegistry validatorRegistry;
    private SandboxProvider sandboxProvider;
    private QueryExecutor queryExecutor;
    private ExplainProvider explainProvider;
    private ExecutorService workerPool;
    private ExecutionService service;

    private static final ExecutionPolicy POLICY = new ExecutionPolicy(
            5000, 500, 1_048_576, java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(3), 2, 8, 3);
    private static final AuthenticatedUser USER = new AuthenticatedUser(TypedId.of("user-1"), Set.of("learner"), "access");

    @BeforeEach
    void setUp() {
        repository = mock(ExecutionRepository.class);
        validatorRegistry = mock(QueryValidatorRegistry.class);
        sandboxProvider = mock(SandboxProvider.class);
        queryExecutor = mock(QueryExecutor.class);
        explainProvider = mock(ExplainProvider.class);
        workerPool = Executors.newFixedThreadPool(4);

        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.next()).thenReturn("exec-1", "exec-2", "exec-3", "exec-4", "exec-5");
        when(idGenerator.nextTyped()).thenAnswer(inv -> TypedId.of(idGenerator.next()));
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

        service = new ExecutionService(
                repository, validatorRegistry, sandboxProvider, queryExecutor, explainProvider,
                new ExecutionAuditLogger(), idGenerator, clock, POLICY, workerPool, new Semaphore(8, true));

        // repository.replace is the only thing that needs to "persist" across calls for
        // findById-based re-reads inside the service (transitionTo/finishX read-modify-write).
        var store = new java.util.concurrent.ConcurrentHashMap<String, Execution>();
        org.mockito.Mockito.doAnswer(inv -> {
            Execution e = inv.getArgument(0);
            store.put(e.id().value(), e);
            return null;
        }).when(repository).insert(any());
        org.mockito.Mockito.doAnswer(inv -> {
            Execution e = inv.getArgument(0);
            store.put(e.id().value(), e);
            return null;
        }).when(repository).replace(any());
        when(repository.findById(any())).thenAnswer(inv -> {
            TypedId<Execution> id = inv.getArgument(0);
            return Optional.ofNullable(store.get(id.value()));
        });
    }

    @AfterEach
    void tearDown() {
        workerPool.shutdownNow();
    }

    @Test
    void createRejectsAnInvalidStatementSynchronouslyWithoutTouchingTheSandbox() {
        when(validatorRegistry.resolve(EngineType.POSTGRES)).thenReturn(fixedValidator(ValidationResult.reject("no DROP allowed")));

        Execution result = service.create(USER, EngineType.POSTGRES, "two-sum", Optional.empty(), "DROP TABLE orders");

        assertThat(result.status()).isEqualTo(ExecutionStatus.REJECTED);
        assertThat(result.rejectionReason()).contains("no DROP allowed");
        verifyNoInteractions(sandboxProvider, queryExecutor);
    }

    @Test
    void createRunsAValidStatementThroughToCompleted() {
        when(validatorRegistry.resolve(EngineType.POSTGRES)).thenReturn(fixedValidator(ValidationResult.allow()));
        SessionHandle session = new SessionHandle("sess-1", EngineType.POSTGRES, "DBArena_sess1");
        when(sandboxProvider.acquire(any(), anyString(), any())).thenReturn(session);

        ExecutionResultSummary summary = new ExecutionResultSummary(
                List.of(new ColumnMeta("id", "Int", false)), List.of(List.of("1")), false);
        ExecutionMetrics metrics = new ExecutionMetrics(10L, Optional.empty(), 1, 16L);
        ExecutionResult raw = new ExecutionResult(summary.columns(), List.of(), 10L, Optional.empty());
        when(queryExecutor.execute(any(), any(), anyString(), any()))
                .thenReturn(new QueryExecutionOutcome(raw, Optional.of(new com.DBArena.services.execution.evaluator.ResultEvaluator.Evaluation(summary, metrics))));

        Execution created = service.create(USER, EngineType.POSTGRES, "two-sum", Optional.empty(), "SELECT 1");
        assertThat(created.status()).isEqualTo(ExecutionStatus.QUEUED);

        ArgumentCaptor<Execution> captor = ArgumentCaptor.forClass(Execution.class);
        verify(repository, timeout(2000).atLeastOnce()).replace(captor.capture());
        assertThat(captor.getAllValues()).extracting(Execution::status).contains(ExecutionStatus.COMPLETED);
        verify(sandboxProvider, timeout(2000)).release(session);
    }

    @Test
    void createEnforcesThePerUserConcurrencyLimit() throws InterruptedException {
        when(validatorRegistry.resolve(EngineType.POSTGRES)).thenReturn(fixedValidator(ValidationResult.allow()));
        when(sandboxProvider.acquire(any(), anyString(), any()))
                .thenReturn(new SessionHandle("sess-1", EngineType.POSTGRES, "DBArena_sess1"));

        CountDownLatch release = new CountDownLatch(1);
        when(queryExecutor.execute(any(), any(), anyString(), any())).thenAnswer(inv -> {
            release.await(5, TimeUnit.SECONDS);
            return new QueryExecutionOutcome(new ExecutionResult(List.of(), List.of(), 1L, Optional.empty()), Optional.empty());
        });

        service.create(USER, EngineType.POSTGRES, "two-sum", Optional.empty(), "SELECT 1"); // slot 1/2
        service.create(USER, EngineType.POSTGRES, "two-sum", Optional.empty(), "SELECT 2"); // slot 2/2
        // give the workers a moment to actually enter queryExecutor.execute and hold their slots
        Thread.sleep(200);

        assertThatThrownBy(() -> service.create(USER, EngineType.POSTGRES, "two-sum", Optional.empty(), "SELECT 3"))
                .isInstanceOf(TooManyConcurrentExecutionsException.class);

        release.countDown();
    }

    @Test
    void getOwnedThrowsNotFoundForAnotherUsersExecution() {
        AuthenticatedUser otherUser = new AuthenticatedUser(TypedId.of("user-2"), Set.of("learner"), "access");
        Execution owned = Execution.requested(
                TypedId.of("exec-1"), TypedId.of(otherUser.userId().value()), EngineType.POSTGRES,
                "two-sum", Optional.empty(), "SELECT 1", 0L);
        when(repository.findById(TypedId.of("exec-1"))).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.getOwned(USER, TypedId.of("exec-1")))
                .isInstanceOf(ExecutionNotFoundException.class);
    }

    @Test
    void cancelOnATerminalExecutionIsANoOp() {
        Execution completed = Execution.requested(
                        TypedId.of("exec-1"), TypedId.of(USER.userId().value()), EngineType.POSTGRES,
                        "two-sum", Optional.empty(), "SELECT 1", 0L)
                .withCompleted(new ExecutionResultSummary(List.of(), List.of(), false), new ExecutionMetrics(1L, Optional.empty(), 0, 0L), 1L);
        when(repository.findById(TypedId.of("exec-1"))).thenReturn(Optional.of(completed));

        Execution result = service.cancel(USER, TypedId.of("exec-1"));

        assertThat(result.status()).isEqualTo(ExecutionStatus.COMPLETED);
        verifyNoInteractions(sandboxProvider, queryExecutor);
    }

    private static com.DBArena.services.execution.validation.QueryValidator fixedValidator(ValidationResult result) {
        return new com.DBArena.services.execution.validation.QueryValidator() {
            @Override
            public EngineType engineType() {
                return EngineType.POSTGRES;
            }

            @Override
            public ValidationResult validate(String statementText, ExecutionPolicy policy) {
                return result;
            }
        };
    }
}
