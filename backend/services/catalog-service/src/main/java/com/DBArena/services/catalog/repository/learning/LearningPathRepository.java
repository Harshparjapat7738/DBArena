package com.DBArena.services.catalog.repository.learning;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.learning.LearningPath;

import java.util.Optional;

public interface LearningPathRepository {

    void insert(LearningPath path);

    void replace(LearningPath path);

    Optional<LearningPath> findBySlug(String slug);

    boolean existsBySlug(String slug);

    CursorPage<LearningPath> findPage(PageRequest pageRequest);
}
