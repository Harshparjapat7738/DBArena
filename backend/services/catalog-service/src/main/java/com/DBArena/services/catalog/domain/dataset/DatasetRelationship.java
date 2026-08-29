package com.DBArena.services.catalog.domain.dataset;

public record DatasetRelationship(String toEntity, String type) {

    public DatasetRelationship {
        if (toEntity == null || toEntity.isBlank()) {
            throw new IllegalArgumentException("toEntity must not be blank");
        }
    }
}
