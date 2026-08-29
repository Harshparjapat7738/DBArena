package com.DBArena.services.execution.web.dto;

import com.DBArena.engine.spi.model.ExplainPlan;

public record ExplainResponse(String rawPlanText, Long estimatedCost) {

    public static ExplainResponse from(ExplainPlan plan) {
        return new ExplainResponse(plan.rawPlanText(), plan.estimatedCost().orElse(null));
    }
}
