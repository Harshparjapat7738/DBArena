package com.dbforge.engine.spi.model;

/**
 * Minimal placeholder for "which dataset, at which version, to
 * materialize". The full Canonical Dataset Model - entities, columns,
 * relationships, seed rows - is defined by milestone B02
 * ({@code tools/dataset-cli}) per docs/02 (not yet written; ask before
 * assuming its exact shape). This record exists now only so
 * {@link com.dbforge.engine.spi.DatabaseEngineAdapter#materialize} has a
 * parameter type to compile against; expect B02 to either extend this or
 * replace it with a richer type once the CDM shape is settled.
 */
public record DatasetDescriptor(String datasetId, String name, int schemaVersion) {

    public DatasetDescriptor {
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException("datasetId must not be blank");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
    }
}
