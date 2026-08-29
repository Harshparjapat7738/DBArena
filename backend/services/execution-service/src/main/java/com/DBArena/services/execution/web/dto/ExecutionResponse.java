package com.DBArena.services.execution.web.dto;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.execution.domain.Execution;
import com.DBArena.services.execution.domain.ExecutionStatus;

public record ExecutionResponse(
        String id,
        ExecutionStatus status,
        EngineType engine,
        String datasetSlug,
        String problemSlug,
        String statementText,
        long requestedAtEpochMillis,
        Long startedAtEpochMillis,
        Long completedAtEpochMillis,
        ExecutionResultResponse result,
        ExecutionMetricsResponse metrics,
        String rejectionReason,
        String errorMessage) {

    public static ExecutionResponse from(Execution execution) {
        return new ExecutionResponse(
                execution.id().value(),
                execution.status(),
                execution.engine(),
                execution.datasetSlug(),
                execution.problemSlug().orElse(null),
                execution.statementText(),
                execution.requestedAtEpochMillis(),
                execution.startedAtEpochMillis().orElse(null),
                execution.completedAtEpochMillis().orElse(null),
                execution.result().map(ExecutionResultResponse::from).orElse(null),
                execution.metrics().map(ExecutionMetricsResponse::from).orElse(null),
                execution.rejectionReason().orElse(null),
                execution.errorMessage().orElse(null));
    }
}
