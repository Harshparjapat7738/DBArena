package com.DBArena.services.execution.executor;

import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.services.execution.evaluator.ResultEvaluator;

import java.util.Optional;

/** Either {@code evaluation} (success) or {@code raw.error()} (failure) is meaningful - never both, mirroring {@link ExecutionResult}'s own success/failure shape one level up. */
public record QueryExecutionOutcome(ExecutionResult raw, Optional<ResultEvaluator.Evaluation> evaluation) {

    public boolean isSuccess() {
        return raw.isSuccess();
    }
}
