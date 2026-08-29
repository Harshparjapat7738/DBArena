package com.DBArena.services.execution.evaluator;

import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.services.execution.domain.ExecutionMetrics;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.domain.ExecutionResultSummary;

import java.util.Optional;

/**
 * Turns an engine-native {@link ExecutionResult} into a capped,
 * safe-to-serialize {@link ExecutionResultSummary} plus its
 * {@link ExecutionMetrics} - the "no hidden test-case exposure" boundary:
 * this is the one place a future reference-solution comparison (B10) would
 * plug in, and it would compare here, server-side, never by handing the
 * learner's response an expected dataset to diff against client-side.
 */
public interface ResultEvaluator {

    record Evaluation(ExecutionResultSummary summary, ExecutionMetrics metrics) {
    }

    Evaluation evaluate(ExecutionResult raw, ExecutionPolicy policy, Optional<Long> planningTimeMillis);
}
