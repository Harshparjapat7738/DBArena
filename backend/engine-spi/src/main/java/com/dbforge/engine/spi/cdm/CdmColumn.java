package com.dbforge.engine.spi.cdm;

/** One column declaration on a {@link CdmEntity}. A primary-key column may never be nullable. */
public record CdmColumn(String name, CdmType type, boolean nullable, boolean primaryKey) {

    public CdmColumn {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("column name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("column type must not be null");
        }
        if (primaryKey && nullable) {
            throw new IllegalArgumentException("column '" + name + "' cannot be both primaryKey and nullable");
        }
    }
}
