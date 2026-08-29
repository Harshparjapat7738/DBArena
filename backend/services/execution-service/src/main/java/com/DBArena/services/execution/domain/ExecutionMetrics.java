package com.DBArena.services.execution.domain;

import java.util.Optional;

/**
 * Per the task brief: execution time, planning time when available, rows
 * returned, result size, status (status lives on {@link Execution} itself,
 * not duplicated here). {@code planningTimeMillis} is only ever populated
 * when a best-effort {@code EXPLAIN (ANALYZE, FORMAT JSON)} re-run
 * succeeds within its own short budget after the real execution already
 * completed - see {@code DefaultQueryExecutor}'s Javadoc; its absence is
 * normal, not an error.
 */
public record ExecutionMetrics(
        long executionTimeMillis,
        Optional<Long> planningTimeMillis,
        int rowsReturned,
        long resultSizeBytes) {

    public ExecutionMetrics {
        planningTimeMillis = planningTimeMillis == null ? Optional.empty() : planningTimeMillis;
        if (executionTimeMillis < 0) {
            throw new IllegalArgumentException("executionTimeMillis must not be negative");
        }
        if (rowsReturned < 0) {
            throw new IllegalArgumentException("rowsReturned must not be negative");
        }
        if (resultSizeBytes < 0) {
            throw new IllegalArgumentException("resultSizeBytes must not be negative");
        }
    }
}
