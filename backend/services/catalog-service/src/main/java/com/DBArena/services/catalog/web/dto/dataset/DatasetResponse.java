package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;

import java.util.List;
import java.util.Set;

/** Mirrors the frontend mock's {@code Dataset} shape field-for-field (slug/name/description/category/engines/entities/rowCountLabel/problemCount). */
public record DatasetResponse(
        String slug,
        String name,
        String description,
        String category,
        Set<EngineType> engines,
        List<DatasetEntityResponse> entities,
        String rowCountLabel,
        int problemCount) {

    /** Full detail, sample rows included - {@code GET /api/v1/datasets/{slug}}. */
    public static DatasetResponse full(DatasetMetadata dataset, int problemCount) {
        return new DatasetResponse(dataset.slug(), dataset.name(), dataset.description(), dataset.category(),
                dataset.engines(), dataset.entities().stream().map(DatasetEntityResponse::from).toList(),
                dataset.rowCountLabel(), problemCount);
    }

    /** List/browse shape - schema only, sample rows stripped to keep a page of results light. */
    public static DatasetResponse forListing(DatasetMetadata dataset, int problemCount) {
        return new DatasetResponse(dataset.slug(), dataset.name(), dataset.description(), dataset.category(),
                dataset.engines(), dataset.entities().stream().map(DatasetEntityResponse::withoutSampleRows).toList(),
                dataset.rowCountLabel(), problemCount);
    }
}
