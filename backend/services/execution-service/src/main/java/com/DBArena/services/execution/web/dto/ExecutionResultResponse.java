package com.DBArena.services.execution.web.dto;

import com.DBArena.engine.spi.model.ColumnMeta;
import com.DBArena.services.execution.domain.ExecutionResultSummary;

import java.util.List;

public record ExecutionResultResponse(List<ColumnMeta> columns, List<List<String>> rows, boolean truncated) {

    public static ExecutionResultResponse from(ExecutionResultSummary summary) {
        return new ExecutionResultResponse(summary.columns(), summary.rows(), summary.truncated());
    }
}
