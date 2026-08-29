package com.DBArena.engine.spi.model;

import java.util.List;
import java.util.Optional;

/**
 * Either a successful result set ({@code error} empty, {@code columns}/{@code rows}
 * populated) or a failure ({@code error} present, {@code columns}/{@code rows} empty).
 * Modeled as one record rather than a sealed success/failure pair to keep
 * adapter implementations simple; callers that want the stricter shape
 * can lift this into a {@code Result<ExecutionResult, ExecutionError>}.
 */
public record ExecutionResult(
        List<ColumnMeta> columns,
        List<ResultRow> rows,
        long executionTimeMillis,
        Optional<ExecutionError> error) {

    public ExecutionResult {
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        error = error == null ? Optional.empty() : error;
        if (executionTimeMillis < 0) {
            throw new IllegalArgumentException("executionTimeMillis must be >= 0");
        }
    }

    public static ExecutionResult failure(ExecutionError error, long executionTimeMillis) {
        return new ExecutionResult(List.of(), List.of(), executionTimeMillis, Optional.of(error));
    }

    public boolean isSuccess() {
        return error.isEmpty();
    }
}
