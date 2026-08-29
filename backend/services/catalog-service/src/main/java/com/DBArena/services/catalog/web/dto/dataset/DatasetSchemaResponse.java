package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;

import java.util.List;

public record DatasetSchemaResponse(String slug, List<DatasetEntitySchemaResponse> entities) {

    public static DatasetSchemaResponse from(DatasetMetadata dataset) {
        return new DatasetSchemaResponse(
                dataset.slug(), dataset.entities().stream().map(DatasetEntitySchemaResponse::from).toList());
    }
}
