package com.dbforge.services.catalog.repository;

import com.dbforge.common.core.pagination.CursorPage;
import com.dbforge.common.core.pagination.PageRequest;
import com.dbforge.services.catalog.domain.Problem;
import com.dbforge.services.catalog.domain.ProblemFilter;
import com.dbforge.services.catalog.domain.TagCount;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository {

    void insert(Problem problem);

    /** Full-document replace, keyed on id. Used by update/publish/unpublish. */
    void replace(Problem problem);

    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    CursorPage<Problem> findPage(ProblemFilter filter, PageRequest pageRequest);

    /** Tag -> count, across every problem matching {@code publishedOnly} if set. Sorted by count desc. */
    List<TagCount> listTagCounts(boolean publishedOnly);
}
