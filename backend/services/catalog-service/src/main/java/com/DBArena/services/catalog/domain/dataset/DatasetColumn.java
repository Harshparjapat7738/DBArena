package com.DBArena.services.catalog.domain.dataset;

import java.util.Optional;

public record DatasetColumn(String name, String type, boolean nullable, boolean primaryKey, Optional<String> foreignKey) {

    public DatasetColumn {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        foreignKey = foreignKey == null ? Optional.empty() : foreignKey;
    }
}
