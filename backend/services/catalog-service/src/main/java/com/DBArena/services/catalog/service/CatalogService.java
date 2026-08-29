package com.DBArena.services.catalog.service;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.Problem;
import com.DBArena.services.catalog.domain.ProblemFilter;
import com.DBArena.services.catalog.domain.ProblemSort;
import com.DBArena.services.catalog.domain.TagCount;
import com.DBArena.services.catalog.repository.ProblemRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/**
 * No {@code @Transactional} here (unlike identity-service's AuthService) -
 * Mongo single-document writes are already atomic, and this milestone
 * never touches more than one document per operation. All state flows
 * through parameters and return values only, same discipline as
 * AuthService, for the same reason: this is a Spring singleton shared
 * across concurrent requests.
 */
@Service
public class CatalogService {

    private final ProblemRepository problemRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public CatalogService(ProblemRepository problemRepository, IdGenerator idGenerator, Clock clock) {
        this.problemRepository = problemRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public CursorPage<Problem> browsePublishedProblems(ProblemFilter filter, PageRequest pageRequest) {
        return problemRepository.findPage(forcePublished(filter), pageRequest);
    }

    /** B03: {@code /api/v1/problems}, with a caller-chosen sort and the fuller filter (dataset/slug-join clauses). */
    public CursorPage<Problem> browsePublishedProblems(ProblemFilter filter, PageRequest pageRequest, ProblemSort sort) {
        return problemRepository.findPage(forcePublished(filter), pageRequest, sort);
    }

    /** publishedOnly is forced true regardless of what a caller passed in - browsing never sees a draft. */
    private static ProblemFilter forcePublished(ProblemFilter filter) {
        return new ProblemFilter(filter.tag(), filter.difficulty(), filter.engine(), filter.titleSearch(),
                true, filter.datasetSlug(), filter.slugIn(), filter.slugNotIn());
    }

    /**
     * Browsing (GET) only ever sees published problems - an unpublished
     * slug 404s exactly like a slug that doesn't exist at all, rather than
     * leaking its existence. Authoring endpoints use {@link #getAnyProblemBySlug}
     * instead. There is no "preview my own unpublished draft" endpoint in
     * this milestone - flagged as Carried forward.
     */
    public Problem getPublishedProblemBySlug(String slug) {
        Problem problem = getAnyProblemBySlug(slug);
        if (!problem.published()) {
            throw new ProblemNotFoundException(slug);
        }
        return problem;
    }

    public Problem getAnyProblemBySlug(String slug) {
        return problemRepository.findBySlug(slug).orElseThrow(() -> new ProblemNotFoundException(slug));
    }

    public List<TagCount> listPublishedTagCounts() {
        return problemRepository.listTagCounts(true);
    }

    public Problem createProblem(CreateProblemCommand command) {
        if (problemRepository.existsBySlug(command.slug())) {
            throw new DuplicateSlugException(command.slug());
        }
        long now = clock.millis();
        Problem problem = new Problem(
                idGenerator.nextTyped(),
                command.slug(),
                command.title(),
                command.statementMarkdown(),
                command.difficulty(),
                command.tags(),
                command.allowedEngines(),
                command.datasetSlug(),
                false,
                now,
                now);
        problemRepository.insert(problem);
        return problem;
    }

    public Problem updateProblem(String slug, UpdateProblemCommand command) {
        Problem existing = getAnyProblemBySlug(slug);
        Problem updated = existing.withRevisedContent(
                command.title(),
                command.statementMarkdown(),
                command.difficulty(),
                command.tags(),
                command.allowedEngines(),
                command.datasetSlug(),
                clock.millis());
        problemRepository.replace(updated);
        return updated;
    }

    public Problem setPublished(String slug, boolean published) {
        Problem existing = getAnyProblemBySlug(slug);
        Problem updated = existing.withPublished(published, clock.millis());
        problemRepository.replace(updated);
        return updated;
    }
}
