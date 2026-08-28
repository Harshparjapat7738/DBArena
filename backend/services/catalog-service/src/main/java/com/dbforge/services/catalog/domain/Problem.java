package com.dbforge.services.catalog.domain;

import com.dbforge.common.core.id.TypedId;

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
        Set<EngineKind> allowedEngines,
        String datasetSlug,
        boolean published,
        long createdAtEpochMillis,
        long updatedAtEpochMillis) {

    public Problem {
        tags = Set.copyOf(tags);
        allowedEngines = Set.copyOf(allowedEngines);
        if (allowedEngines.isEmpty()) {
            throw new IllegalArgumentException("a problem must allow at least one engine");
        }
    }

    public Problem withPublished(boolean newPublished, long newUpdatedAtEpochMillis) {
        return new Problem(id, slug, title, statementMarkdown, difficulty, tags, allowedEngines,
                datasetSlug, newPublished, createdAtEpochMillis, newUpdatedAtEpochMillis);
    }
}
