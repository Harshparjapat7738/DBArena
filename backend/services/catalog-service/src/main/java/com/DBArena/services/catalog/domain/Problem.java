package com.DBArena.services.catalog.domain;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;

import java.util.Set;

/**
 * A catalog entry. {@code datasetSlug} is a loose reference (plain string,
 * not a foreign key) to the dataset this problem is authored against -
 * datasets/ (B02) doesn't exist yet, so this can't be a TypedId<Dataset>
 * pointing at a real descriptor until that milestone lands; kept as a
 * string deliberately so a later migration only has to change the type,
 * not the shape.
 *
 * <p>Timestamps are UTC epoch millis (hard rule #9) - stored as a plain
 * {@code long} in Mongo, not a BSON {@code Date}, so there is exactly one
 * timestamp representation anywhere in this platform, not two.
 */
public record Problem(
        TypedId<Problem> id,
        String slug,
        String title,
        String statementMarkdown,
        Difficulty difficulty,
        Set<String> tags,
        Set<EngineType> allowedEngines,
        String datasetSlug,
        boolean published,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        int version) {

    public Problem {
        tags = Set.copyOf(tags);
        allowedEngines = Set.copyOf(allowedEngines);
        if (allowedEngines.isEmpty()) {
            throw new IllegalArgumentException("a problem must allow at least one engine");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }

    /**
     * Back-compat convenience constructor (B02): every call site written before
     * versioning existed passes exactly these 11 args and expects a new,
     * first-version problem - defaults {@code version} to 1 rather than forcing
     * every caller (including tests predating B02) to learn about it.
     */
    public Problem(
            TypedId<Problem> id,
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
        this(id, slug, title, statementMarkdown, difficulty, tags, allowedEngines,
                datasetSlug, published, createdAtEpochMillis, updatedAtEpochMillis, 1);
    }

    public Problem withPublished(boolean newPublished, long newUpdatedAtEpochMillis) {
        return new Problem(id, slug, title, statementMarkdown, difficulty, tags, allowedEngines,
                datasetSlug, newPublished, createdAtEpochMillis, newUpdatedAtEpochMillis, version);
    }

    /** Full-content update stamps a new version - see docs/02 (once it exists) for the eventual version-history story; for B02 this is a bare counter, no snapshot is retained. */
    public Problem withRevisedContent(
            String newTitle,
            String newStatementMarkdown,
            Difficulty newDifficulty,
            Set<String> newTags,
            Set<EngineType> newAllowedEngines,
            String newDatasetSlug,
            long newUpdatedAtEpochMillis) {
        return new Problem(id, slug, newTitle, newStatementMarkdown, newDifficulty, newTags, newAllowedEngines,
                newDatasetSlug, published, createdAtEpochMillis, newUpdatedAtEpochMillis, version + 1);
    }
}
