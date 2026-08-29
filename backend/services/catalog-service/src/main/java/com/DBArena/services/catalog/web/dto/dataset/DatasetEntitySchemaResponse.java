package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.services.catalog.domain.dataset.DatasetEntity;
import com.DBArena.services.catalog.domain.dataset.DatasetEntityKind;

import java.util.List;

public record DatasetEntitySchemaResponse(
        String name,
        DatasetEntityKind kind,
        List<DatasetColumnResponse> columns,
        List<DatasetRelationshipResponse> relationships) {

    public static DatasetEntitySchemaResponse from(DatasetEntity entity) {
        return new DatasetEntitySchemaResponse(
                entity.name(),
                entity.kind(),
                entity.columns().stream().map(DatasetColumnResponse::from).toList(),
                entity.relationships().stream().map(DatasetRelationshipResponse::from).toList());
    }
}
