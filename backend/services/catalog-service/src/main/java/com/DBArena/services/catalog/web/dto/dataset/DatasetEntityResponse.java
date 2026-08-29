package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.services.catalog.domain.dataset.DatasetEntity;
import com.DBArena.services.catalog.domain.dataset.DatasetEntityKind;

import java.util.List;
import java.util.Map;

/** Full shape - used by {@code GET /api/v1/datasets/{slug}} only. List/browse responses use {@link #withoutSampleRows} to stay light. */
public record DatasetEntityResponse(
        String name,
        DatasetEntityKind kind,
        List<DatasetColumnResponse> columns,
        List<Map<String, String>> sampleRows,
        List<DatasetRelationshipResponse> relationships) {

    public static DatasetEntityResponse from(DatasetEntity entity) {
        return new DatasetEntityResponse(
                entity.name(),
                entity.kind(),
                entity.columns().stream().map(DatasetColumnResponse::from).toList(),
                entity.sampleRows(),
                entity.relationships().stream().map(DatasetRelationshipResponse::from).toList());
    }

    public static DatasetEntityResponse withoutSampleRows(DatasetEntity entity) {
        return new DatasetEntityResponse(
                entity.name(),
                entity.kind(),
                entity.columns().stream().map(DatasetColumnResponse::from).toList(),
                List.of(),
                entity.relationships().stream().map(DatasetRelationshipResponse::from).toList());
    }
}
