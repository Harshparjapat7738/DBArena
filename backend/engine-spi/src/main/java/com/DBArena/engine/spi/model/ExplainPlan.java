package com.dbforge.engine.spi.model;

import java.util.Optional;

/** The engine's native explain/plan output plus, if the engine reports one, a comparable cost estimate. */
public record ExplainPlan(String rawPlanText, Optional<Long> estimatedCost) {

    public ExplainPlan {
        if (rawPlanText == null || rawPlanText.isBlank()) {
            throw new IllegalArgumentException("rawPlanText must not be blank");
        }
        estimatedCost = estimatedCost == null ? Optional.empty() : estimatedCost;
    }
}
