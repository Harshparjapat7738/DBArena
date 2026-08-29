package com.dbforge.services.catalog.web.dto;

import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.engine.spi.EngineType;
import com.dbforge.services.catalog.domain.Problem;

import java.util.Set;

/** List-view shape - deliberately omits the (potentially large) statement body. */
public record ProblemSummaryResponse(
        String slug,
        String title,
        Difficulty difficulty,
        Set<String> tags,
        Set<EngineType> allowedEngines) {

    public static ProblemSummaryResponse from(Problem problem) {
        return new ProblemSummaryResponse(
                problem.slug(), problem.title(), problem.difficulty(), problem.tags(), problem.allowedEngines());
    }
}
