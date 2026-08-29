package com.DBArena.services.execution.evaluator;

import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.engine.spi.model.ResultRow;
import com.DBArena.services.execution.domain.ExecutionMetrics;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.domain.ExecutionResultSummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DefaultResultEvaluator implements ResultEvaluator {

    @Override
    public Evaluation evaluate(ExecutionResult raw, ExecutionPolicy policy, Optional<Long> planningTimeMillis) {
        boolean truncated = raw.rows().size() > policy.maxResultRows();
        List<ResultRow> cappedRows = truncated ? raw.rows().subList(0, policy.maxResultRows()) : raw.rows();

        List<List<String>> stringifiedRows = cappedRows.stream()
                .map(row -> row.values().stream().map(CdmValueStringifier::toDisplayString).toList())
                .toList();

        long resultSizeBytes = estimateSizeBytes(stringifiedRows);
        ExecutionResultSummary summary = new ExecutionResultSummary(raw.columns(), stringifiedRows, truncated);
        ExecutionMetrics metrics = new ExecutionMetrics(
                raw.executionTimeMillis(), planningTimeMillis, stringifiedRows.size(), resultSizeBytes);
        return new Evaluation(summary, metrics);
    }

    /** A rough, cheap estimate (sum of stringified cell lengths) - good enough for a resource-hygiene metric, not a billing-grade byte count. */
    private static long estimateSizeBytes(List<List<String>> rows) {
        long total = 0;
        for (List<String> row : rows) {
            for (String cell : row) {
                total += cell == null ? 4 : cell.length();
            }
        }
        return total;
    }
}
