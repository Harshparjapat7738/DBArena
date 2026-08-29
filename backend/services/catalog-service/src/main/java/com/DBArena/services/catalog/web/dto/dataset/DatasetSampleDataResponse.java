package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;

import java.util.List;

public record DatasetSampleDataResponse(String slug, List<DatasetEntitySampleDataResponse> entities) {

    public static DatasetSampleDataResponse from(DatasetMetadata dataset) {
        return new DatasetSampleDataResponse(
                dataset.slug(), dataset.entities().stream().map(DatasetEntitySampleDataResponse::from).toList());
    }
}
