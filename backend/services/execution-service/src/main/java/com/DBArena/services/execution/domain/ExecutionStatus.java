package com.DBArena.services.execution.domain;

import java.util.Set;

/**
 * The full lifecycle of one execution (B04). Terminal states are
 * {@link #COMPLETED}, {@link #REJECTED}, {@link #TIMEOUT},
 * {@link #RESOURCE_LIMIT}, {@link #ENGINE_ERROR}, {@link #CANCELLED} - once
 * reached, no further transition is valid (enforced by
 * {@code ExecutionService}, not by this enum itself).
 */
public enum ExecutionStatus {
    REQUESTED,
    VALIDATING,
    QUEUED,
    STARTING,
    EXECUTING,
    EVALUATING,
    COMPLETED,
    REJECTED,
    TIMEOUT,
    RESOURCE_LIMIT,
    ENGINE_ERROR,
    CANCELLED;

    private static final Set<ExecutionStatus> TERMINAL =
            Set.of(COMPLETED, REJECTED, TIMEOUT, RESOURCE_LIMIT, ENGINE_ERROR, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}
