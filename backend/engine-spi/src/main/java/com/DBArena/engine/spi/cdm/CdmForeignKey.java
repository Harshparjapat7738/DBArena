package com.DBArena.engine.spi.cdm;

/**
 * A single-column reference from the declaring entity's {@code columnName}
 * to {@code referencesEntity.referencesColumn}. Composite (multi-column)
 * foreign keys are out of scope for this milestone - see backend/CLAUDE.md
 * M02 Session Log "Carried forward" if one is ever needed.
 */
public record CdmForeignKey(String columnName, String referencesEntity, String referencesColumn) {

    public CdmForeignKey {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("columnName must not be blank");
        }
        if (referencesEntity == null || referencesEntity.isBlank()) {
            throw new IllegalArgumentException("referencesEntity must not be blank");
        }
        if (referencesColumn == null || referencesColumn.isBlank()) {
            throw new IllegalArgumentException("referencesColumn must not be blank");
        }
    }
}
