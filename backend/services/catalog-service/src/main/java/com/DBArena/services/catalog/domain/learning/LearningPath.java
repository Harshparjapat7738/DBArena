package com.DBArena.services.catalog.domain.learning;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;

import java.util.List;

/**
 * A structured sequence of {@link Lesson}s (B02). Lessons are embedded, not a
 * child collection - see {@link Lesson}'s Javadoc for why.
 */
public record LearningPath(
        TypedId<LearningPath> id,
        String slug,
        String title,
        String description,
        LearningLevel level,
        EngineType engine,
        List<Lesson> lessons,
        int estimatedHours,
        int version,
        long createdAtEpochMillis,
        long updatedAtEpochMillis) {

    public LearningPath {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        lessons = List.copyOf(lessons);
        if (estimatedHours < 0) {
            throw new IllegalArgumentException("estimatedHours must not be negative");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }

    public LearningPath withRevisedContent(
            String newTitle,
            String newDescription,
            LearningLevel newLevel,
            EngineType newEngine,
            List<Lesson> newLessons,
            int newEstimatedHours,
            long newUpdatedAtEpochMillis) {
        return new LearningPath(id, slug, newTitle, newDescription, newLevel, newEngine, newLessons,
                newEstimatedHours, version + 1, createdAtEpochMillis, newUpdatedAtEpochMillis);
    }
}
