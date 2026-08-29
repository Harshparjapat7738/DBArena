package com.DBArena.services.execution.evaluator;

import com.DBArena.common.core.value.CdmValue;
import com.DBArena.engine.spi.model.ColumnMeta;
import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.engine.spi.model.ResultRow;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultResultEvaluatorTest {

    private final DefaultResultEvaluator evaluator = new DefaultResultEvaluator();

    private static ExecutionPolicy policyWithRowLimit(int maxRows) {
        return new ExecutionPolicy(5000, maxRows, 1_048_576, Duration.ofSeconds(5), Duration.ofSeconds(3), 2, 8, 3);
    }

    private static ResultRow row(long value) {
        return new ResultRow(List.of(new CdmValue.Int(value)));
    }

    @Test
    void truncatesWhenMoreRowsThanThePolicyAllows() {
        List<ColumnMeta> columns = List.of(new ColumnMeta("id", "Int", false));
        ExecutionResult raw = new ExecutionResult(columns, List.of(row(1), row(2), row(3)), 42L, Optional.empty());

        var evaluation = evaluator.evaluate(raw, policyWithRowLimit(2), Optional.empty());

        assertThat(evaluation.summary().truncated()).isTrue();
        assertThat(evaluation.summary().rows()).hasSize(2);
        assertThat(evaluation.metrics().rowsReturned()).isEqualTo(2);
    }

    @Test
    void doesNotFlagTruncatedWhenRowCountIsExactlyTheLimit() {
        List<ColumnMeta> columns = List.of(new ColumnMeta("id", "Int", false));
        ExecutionResult raw = new ExecutionResult(columns, List.of(row(1), row(2)), 10L, Optional.empty());

        var evaluation = evaluator.evaluate(raw, policyWithRowLimit(2), Optional.empty());

        assertThat(evaluation.summary().truncated()).isFalse();
        assertThat(evaluation.summary().rows()).hasSize(2);
    }

    @Test
    void carriesExecutionTimeAndOptionalPlanningTimeIntoMetrics() {
        ExecutionResult raw = new ExecutionResult(List.of(), List.of(), 55L, Optional.empty());

        var evaluation = evaluator.evaluate(raw, policyWithRowLimit(10), Optional.of(7L));

        assertThat(evaluation.metrics().executionTimeMillis()).isEqualTo(55L);
        assertThat(evaluation.metrics().planningTimeMillis()).contains(7L);
    }

    @Test
    void planningTimeIsAbsentWhenNotCaptured() {
        ExecutionResult raw = new ExecutionResult(List.of(), List.of(), 55L, Optional.empty());

        var evaluation = evaluator.evaluate(raw, policyWithRowLimit(10), Optional.empty());

        assertThat(evaluation.metrics().planningTimeMillis()).isEmpty();
    }
}
