package com.DBArena.services.execution.web.dto;

import com.DBArena.services.execution.domain.ExecutionMetrics;

public record ExecutionMetricsResponse(long executionTimeMillis, Long planningTimeMillis, int rowsReturned, long resultSizeBytes) {

    public static ExecutionMetricsResponse from(ExecutionMetrics metrics) {
        return new ExecutionMetricsResponse(
                metrics.executionTimeMillis(), metrics.planningTimeMillis().orElse(null),
                metrics.rowsReturned(), metrics.resultSizeBytes());
    }
}
