package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.services.catalog.domain.dataset.DatasetRelationship;

public record DatasetRelationshipResponse(String toEntity, String type) {

    public static DatasetRelationshipResponse from(DatasetRelationship relationship) {
        return new DatasetRelationshipResponse(relationship.toEntity(), relationship.type());
    }
}
