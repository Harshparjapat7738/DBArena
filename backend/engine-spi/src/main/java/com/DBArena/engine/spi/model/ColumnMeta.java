package com.DBArena.engine.spi.model;

/**
 * One result-set column. {@code cdmTypeName} names a {@code CdmValue}
 * variant (e.g. {@code "Decimal"}, {@code "Timestamp"}) rather than the
 * engine's native type name - callers should never branch on a
 * Postgres/Mongo-specific type string.
 */
public record ColumnMeta(String name, String cdmTypeName, boolean nullable) {

    public ColumnMeta {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (cdmTypeName == null || cdmTypeName.isBlank()) {
            throw new IllegalArgumentException("cdmTypeName must not be blank");
        }
    }
}
