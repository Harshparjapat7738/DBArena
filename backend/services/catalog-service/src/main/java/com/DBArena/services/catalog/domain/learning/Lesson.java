package com.DBArena.services.catalog.domain.learning;

import java.util.Optional;

/**
 * Embedded within {@link LearningPath}, never a standalone collection - a
 * lesson has no independent lifecycle or identity outside its path, and the
 * frontend always fetches a path with all of its lessons together (see B01's
 * audit of {@code learningRepository.listPaths}/{@code getPath}).
 * {@code practiceProblemSlug} is a loose reference (plain string, same
 * convention as {@code Problem.datasetSlug()}) to a catalog-service Problem,
 * not a foreign key.
 */
public record Lesson(
        String slug,
        String title,
        String summary,
        int durationMinutes,
        int order,
        Optional<String> practiceProblemSlug) {

    public Lesson {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("durationMinutes must not be negative");
        }
        practiceProblemSlug = practiceProblemSlug == null ? Optional.empty() : practiceProblemSlug;
    }
}
