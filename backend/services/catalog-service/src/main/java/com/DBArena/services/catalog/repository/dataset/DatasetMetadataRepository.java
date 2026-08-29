package com.DBArena.services.catalog.repository.dataset;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.dataset.DatasetFilter;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;

import java.util.Optional;

public interface DatasetMetadataRepository {

    void insert(DatasetMetadata dataset);

    void replace(DatasetMetadata dataset);

    Optional<DatasetMetadata> findBySlug(String slug);

    boolean existsBySlug(String slug);

    CursorPage<DatasetMetadata> findPage(DatasetFilter filter, PageRequest pageRequest);
}
