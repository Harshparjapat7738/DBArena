package com.DBArena.services.ai.client;

import java.util.Set;

/**
 * Mirrors catalog-service's {@code ProblemDetailResponse} - only the
 * fields the hint context actually needs are declared; Jackson ignores
 * the rest of the JSON body by default (no
 * {@code @JsonIgnoreProperties(ignoreUnknown = false)} anywhere), so this
 * stays a narrow, intentionally-partial view rather than a duplicate of
 * catalog-service's full DTO that has to be kept in lockstep field-for-
 * field.
 */
public record CatalogProblemResponse(
        String slug,
        String title,
        String statementMarkdown,
        String difficulty,
        Set<String> tags,
        Set<String> allowedEngines,
        String datasetSlug,
        boolean published) {
}
