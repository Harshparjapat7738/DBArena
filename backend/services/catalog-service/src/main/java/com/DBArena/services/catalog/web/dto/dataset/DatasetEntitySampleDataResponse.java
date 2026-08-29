package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.services.catalog.domain.dataset.DatasetEntity;

import java.util.List;
import java.util.Map;

public record DatasetEntitySampleDataResponse(String name, List<Map<String, String>> sampleRows) {

    public static DatasetEntitySampleDataResponse from(DatasetEntity entity) {
        return new DatasetEntitySampleDataResponse(entity.name(), entity.sampleRows());
    }
}
