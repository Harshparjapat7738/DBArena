package com.DBArena.services.catalog.domain.dataset;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;

import java.util.List;
import java.util.Set;

/**
 * Read-model over a CDM dataset descriptor (M02's {@code datasets/} tree on
 * disk) so the catalog can expose "browse this dataset's schema" without any
 * caller touching the filesystem - flagged as a gap in B01's audit. This
 * record does not replace the CDM descriptor as the source of truth for
 * materialization; it is a queryable projection of it (name/description/
 * engines/shape), refreshed whenever dataset-cli publishes a new version.
 * {@code entityCount}/{@code rowCountLabel} are display-only summaries, not
 * derived here.
 *
 * <p>{@code version} tracks the CDM dataset's own version so a problem could
 * one day pin an exact dataset version rather than always floating to
 * "latest" - full version history (a superseded-by chain) is intentionally
 * not built in B02; only the current version's projection is stored, one
 * document per {@code slug}.
 */
public record DatasetMetadata(
        TypedId<DatasetMetadata> id,
        String slug,
        String name,
        String description,
        String category,
        Set<EngineType> engines,
        int entityCount,
        String rowCountLabel,
        int version,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        List<DatasetEntity> entities) {

    public DatasetMetadata {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        engines = Set.copyOf(engines);
        if (engines.isEmpty()) {
            throw new IllegalArgumentException("a dataset must target at least one engine");
        }
        if (entityCount < 0) {
            throw new IllegalArgumentException("entityCount must not be negative");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        entities = List.copyOf(entities);
    }

    /**
     * Back-compat convenience constructor (B03) - B02's 11-arg shape had no
     * schema/sample-row detail at all; defaults {@code entities} to empty
     * rather than forcing every caller (including B02's own tests) to learn
     * about it.
     */
    public DatasetMetadata(
            TypedId<DatasetMetadata> id,
            String slug,
            String name,
            String description,
            String category,
            Set<EngineType> engines,
            int entityCount,
            String rowCountLabel,
            int version,
            long createdAtEpochMillis,
            long updatedAtEpochMillis) {
        this(id, slug, name, description, category, engines, entityCount, rowCountLabel, version,
                createdAtEpochMillis, updatedAtEpochMillis, List.of());
    }

    public DatasetMetadata withRevisedContent(
            String newName,
            String newDescription,
            String newCategory,
            Set<EngineType> newEngines,
            int newEntityCount,
            String newRowCountLabel,
            long newUpdatedAtEpochMillis) {
        return new DatasetMetadata(id, slug, newName, newDescription, newCategory, newEngines,
                newEntityCount, newRowCountLabel, version + 1, createdAtEpochMillis, newUpdatedAtEpochMillis, entities);
    }

    /** B03: replaces the schema/sample-row detail wholesale - entities have no independent versioning of their own. */
    public DatasetMetadata withEntities(List<DatasetEntity> newEntities, long newUpdatedAtEpochMillis) {
        return new DatasetMetadata(id, slug, name, description, category, engines, newEntities.size(),
                rowCountLabel, version + 1, createdAtEpochMillis, newUpdatedAtEpochMillis, newEntities);
    }
}
