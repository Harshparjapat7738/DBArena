package com.DBArena.services.catalog.repository;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.Problem;
import com.DBArena.services.catalog.domain.ProblemFilter;
import com.DBArena.services.catalog.domain.ProblemSort;
import com.DBArena.services.catalog.domain.TagCount;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProblemRepository {

    void insert(Problem problem);

    /** Full-document replace, keyed on id. Used by update/publish/unpublish. */
    void replace(Problem problem);

    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** {@code /api/v1/catalog/problems}'s original page shape - oldest-first, unchanged since M13. */
    default CursorPage<Problem> findPage(ProblemFilter filter, PageRequest pageRequest) {
        return findPage(filter, pageRequest, com.DBArena.services.catalog.domain.ProblemSort.OLDEST_FIRST);
    }

    /** B03: {@code /api/v1/problems} uses this with a caller-chosen sort. */
    CursorPage<Problem> findPage(ProblemFilter filter, PageRequest pageRequest, ProblemSort sort);

    /** Tag -> count, across every problem matching {@code publishedOnly} if set. Sorted by count desc. */
    List<TagCount> listTagCounts(boolean publishedOnly);

    /** Published problems sharing {@code datasetSlug} and/or any of {@code tags}, excluding {@code excludeSlug} - unranked candidates; the caller scores and sorts (see {@code ProblemsController#related}). */
    List<Problem> findRelatedCandidates(String datasetSlug, Set<String> tags, String excludeSlug, int candidateLimit);

    /** Published problems authored against a dataset - backs {@code Dataset.problemCount}. */
    long countPublishedByDatasetSlug(String datasetSlug);
}
