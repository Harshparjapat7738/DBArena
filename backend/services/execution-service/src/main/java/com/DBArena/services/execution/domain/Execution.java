package com.DBArena.services.execution.domain;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.engine.spi.EngineType;

import java.util.Optional;

/**
 * One learner's practice-query execution, end to end. Immutable - every
 * transition ({@code ExecutionService}) produces a new value via one of the
 * {@code withX} methods below, which is what gets persisted (see
 * {@code ExecutionRepository#replace}).
 */
public record Execution(
        TypedId<Execution> id,
        TypedId<AuthenticatedUser> userId,
        EngineType engine,
        String datasetSlug,
        Optional<String> problemSlug,
        String statementText,
        ExecutionStatus status,
        long requestedAtEpochMillis,
        Optional<Long> startedAtEpochMillis,
        Optional<Long> completedAtEpochMillis,
        Optional<ExecutionResultSummary> result,
        Optional<ExecutionMetrics> metrics,
        Optional<String> rejectionReason,
        Optional<String> errorMessage) {

    public Execution {
        if (datasetSlug == null || datasetSlug.isBlank()) {
            throw new IllegalArgumentException("datasetSlug must not be blank");
        }
        if (statementText == null || statementText.isBlank()) {
            throw new IllegalArgumentException("statementText must not be blank");
        }
        problemSlug = problemSlug == null ? Optional.empty() : problemSlug;
        startedAtEpochMillis = startedAtEpochMillis == null ? Optional.empty() : startedAtEpochMillis;
        completedAtEpochMillis = completedAtEpochMillis == null ? Optional.empty() : completedAtEpochMillis;
        result = result == null ? Optional.empty() : result;
        metrics = metrics == null ? Optional.empty() : metrics;
        rejectionReason = rejectionReason == null ? Optional.empty() : rejectionReason;
        errorMessage = errorMessage == null ? Optional.empty() : errorMessage;
    }

    public static Execution requested(
            TypedId<Execution> id,
            TypedId<AuthenticatedUser> userId,
            EngineType engine,
            String datasetSlug,
            Optional<String> problemSlug,
            String statementText,
            long nowEpochMillis) {
        return new Execution(id, userId, engine, datasetSlug, problemSlug, statementText, ExecutionStatus.REQUESTED,
                nowEpochMillis, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    /** Plain in-flight transition (VALIDATING/QUEUED/STARTING/EXECUTING/EVALUATING) - no terminal data attached yet. */
    public Execution withStatus(ExecutionStatus newStatus) {
        return new Execution(id, userId, engine, datasetSlug, problemSlug, statementText, newStatus,
                requestedAtEpochMillis, startedAtEpochMillis, completedAtEpochMillis, result, metrics,
                rejectionReason, errorMessage);
    }

    public Execution withStarted(long nowEpochMillis) {
        return new Execution(id, userId, engine, datasetSlug, problemSlug, statementText, ExecutionStatus.STARTING,
                requestedAtEpochMillis, Optional.of(nowEpochMillis), completedAtEpochMillis, result, metrics,
                rejectionReason, errorMessage);
    }

    public Execution withRejected(String reason, long nowEpochMillis) {
        return new Execution(id, userId, engine, datasetSlug, problemSlug, statementText, ExecutionStatus.REJECTED,
                requestedAtEpochMillis, startedAtEpochMillis, Optional.of(nowEpochMillis), result, metrics,
                Optional.of(reason), errorMessage);
    }

    public Execution withCompleted(ExecutionResultSummary newResult, ExecutionMetrics newMetrics, long nowEpochMillis) {
        return new Execution(id, userId, engine, datasetSlug, problemSlug, statementText, ExecutionStatus.COMPLETED,
                requestedAtEpochMillis, startedAtEpochMillis, Optional.of(nowEpochMillis), Optional.of(newResult),
                Optional.of(newMetrics), rejectionReason, errorMessage);
    }

    public Execution withFailure(ExecutionStatus terminalStatus, String message, long nowEpochMillis) {
        if (!terminalStatus.isTerminal()) {
            throw new IllegalArgumentException(terminalStatus + " is not a terminal status");
        }
        return new Execution(id, userId, engine, datasetSlug, problemSlug, statementText, terminalStatus,
                requestedAtEpochMillis, startedAtEpochMillis, Optional.of(nowEpochMillis), result, metrics,
                rejectionReason, Optional.of(message));
    }

    public Execution withCancelled(long nowEpochMillis) {
        return new Execution(id, userId, engine, datasetSlug, problemSlug, statementText, ExecutionStatus.CANCELLED,
                requestedAtEpochMillis, startedAtEpochMillis, Optional.of(nowEpochMillis), result, metrics,
                rejectionReason, errorMessage);
    }
}
