package com.DBArena.engine.spi.cdm;

import java.util.List;
import java.util.Optional;

/**
 * The Canonical Dataset Model root: a dataset authored once, engine-neutral,
 * materialized into every {@link com.DBArena.engine.spi.EngineType} by an
 * adapter's {@code materialize} (see {@link com.DBArena.engine.spi.DatabaseEngineAdapter}).
 * There is deliberately no per-dataset "which engines this targets" field -
 * root CLAUDE.md's premise is that every dataset is materialized into all
 * three; which native type each {@link CdmType} becomes per engine is
 * B03's job (type mapping), not this model's.
 *
 * <p>Replaces engine-spi's M01 {@code DatasetDescriptor} placeholder, per
 * that class's own Javadoc ("expect B02 to ... replace it with a richer
 * type"). Nothing outside engine-spi referenced {@code DatasetDescriptor}
 * yet (no adapter or service consumed it), so this is a clean replacement,
 * not a breaking change to any built code.
 */
public record CdmDataset(String datasetId, String name, int schemaVersion, List<CdmEntity> entities) {

    public CdmDataset {
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException("datasetId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("dataset '" + datasetId + "' must declare at least one entity");
        }
        entities = List.copyOf(entities);
    }

    public Optional<CdmEntity> entity(String entityName) {
        return entities.stream().filter(e -> e.name().equals(entityName)).findFirst();
    }
}
