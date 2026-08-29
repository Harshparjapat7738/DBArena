package com.DBArena.services.catalog.service;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.dataset.DatasetFilter;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;
import com.DBArena.services.catalog.repository.ProblemRepository;
import com.DBArena.services.catalog.repository.dataset.DatasetMetadataRepository;
import org.springframework.stereotype.Service;

/**
 * B03. {@code problemCount} is computed here, not stored on the document -
 * it is derived from {@link ProblemRepository} (a different collection in
 * the same service, not a cross-service call) each time it's asked for,
 * which keeps it always-correct without a denormalized counter that could
 * drift out of sync with the problems collection.
 */
@Service
public class DatasetService {

    private final DatasetMetadataRepository datasetMetadataRepository;
    private final ProblemRepository problemRepository;

    public DatasetService(DatasetMetadataRepository datasetMetadataRepository, ProblemRepository problemRepository) {
        this.datasetMetadataRepository = datasetMetadataRepository;
        this.problemRepository = problemRepository;
    }

    public DatasetMetadata getBySlug(String slug) {
        return datasetMetadataRepository.findBySlug(slug).orElseThrow(() -> new DatasetNotFoundException(slug));
    }

    public CursorPage<DatasetMetadata> browse(DatasetFilter filter, PageRequest pageRequest) {
        return datasetMetadataRepository.findPage(filter, pageRequest);
    }

    public int problemCount(String slug) {
        return (int) problemRepository.countPublishedByDatasetSlug(slug);
    }
}
