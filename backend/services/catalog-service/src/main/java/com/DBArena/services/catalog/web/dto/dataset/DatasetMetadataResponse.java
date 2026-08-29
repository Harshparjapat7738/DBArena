package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;

import java.util.Set;

/**
 * Summary projection only - schema/sample-row browsing (frontend's
 * {@code DatasetEntity[]}, per B01's audit of {@code datasetsRepository.getDataset})
 * is a larger payload deliberately out of scope for this read-model; it
 * needs its own endpoint once something (dataset-cli or a future importer)
 * actually populates entity-level detail here.
 */
public record DatasetMetadataResponse(
        String slug,
        String name,
        String description,
        String category,
        Set<EngineType> engines,
        int entityCount,
        String rowCountLabel,
        int version) {

    public static DatasetMetadataResponse from(DatasetMetadata dataset) {
        return new DatasetMetadataResponse(dataset.slug(), dataset.name(), dataset.description(),
                dataset.category(), dataset.engines(), dataset.entityCount(), dataset.rowCountLabel(), dataset.version());
    }
}
