package com.dbforge.services.catalog.web.dto;

import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.engine.spi.EngineType;
import com.dbforge.services.catalog.domain.Problem;

import java.util.Set;

/**
 * Shared by the public detail endpoint and the admin authoring endpoints -
 * a published problem's full detail is public data by definition (the
 * service layer 404s an unpublished slug before this DTO is ever built for
 * an anonymous caller), so one shape for both is simpler than two nearly
 * identical DTOs.
 */
public record ProblemDetailResponse(
        String slug,
        String title,
        String statementMarkdown,
        Difficulty difficulty,
        Set<String> tags,
        Set<EngineType> allowedEngines,
        String datasetSlug,
        boolean published,
        long createdAtEpochMillis,
        long updatedAtEpochMillis) {

    public static ProblemDetailResponse from(Problem problem) {
        return new ProblemDetailResponse(
                problem.slug(),
                problem.title(),
                problem.statementMarkdown(),
                problem.difficulty(),
                problem.tags(),
                problem.allowedEngines(),
                problem.datasetSlug(),
                problem.published(),
                problem.createdAtEpochMillis(),
                problem.updatedAtEpochMillis());
    }
}
