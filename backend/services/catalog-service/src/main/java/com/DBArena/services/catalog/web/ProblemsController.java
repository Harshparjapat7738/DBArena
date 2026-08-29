package com.DBArena.services.catalog.web;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.web.CurrentUser;
import com.DBArena.common.security.web.CurrentUserArgumentResolver;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.client.SubmissionServiceClient;
import com.DBArena.services.catalog.client.UserServiceClient;
import com.DBArena.services.catalog.domain.Difficulty;
import com.DBArena.services.catalog.domain.Problem;
import com.DBArena.services.catalog.domain.ProblemFilter;
import com.DBArena.services.catalog.domain.ProblemSort;
import com.DBArena.services.catalog.service.CatalogService;
import com.DBArena.services.catalog.service.DatasetService;
import com.DBArena.services.catalog.service.RelatedProblemsRanker;
import com.DBArena.services.catalog.web.dto.ProblemDetailResponse;
import com.DBArena.services.catalog.web.dto.ProblemSummaryResponse;
import com.DBArena.services.catalog.web.dto.dataset.DatasetSampleDataResponse;
import com.DBArena.services.catalog.web.dto.dataset.DatasetSchemaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * B03: a second, richer surface over the exact same {@link CatalogService}/
 * {@link com.DBArena.services.catalog.repository.ProblemRepository} that
 * {@link ProblemController} already uses - no business logic is duplicated,
 * only additional filters/sort/cross-service joins and a different URL
 * shape ({@code /api/v1/problems}, matching the frontend's
 * {@code problemsRepository} contract, vs. {@code /api/v1/catalog/problems}
 * which stays exactly as M13 built it).
 *
 * <p>All-public GETs, same posture as {@link ProblemController}'s browsing
 * endpoints - but {@code bookmarked}/{@code status} require a signed-in
 * caller since they're inherently per-user; requesting either while
 * anonymous 401s via {@link CurrentUserArgumentResolver.UnauthenticatedException}
 * rather than silently ignoring the filter.
 */
@RestController
@RequestMapping("/api/v1/problems")
public class ProblemsController {

    private static final int RELATED_CANDIDATE_LIMIT = 50;
    private static final int RELATED_DEFAULT_LIMIT = 4;

    private final CatalogService catalogService;
    private final DatasetService datasetService;
    private final com.DBArena.services.catalog.repository.ProblemRepository problemRepository;
    private final UserServiceClient userServiceClient;
    private final SubmissionServiceClient submissionServiceClient;

    public ProblemsController(
            CatalogService catalogService,
            DatasetService datasetService,
            com.DBArena.services.catalog.repository.ProblemRepository problemRepository,
            UserServiceClient userServiceClient,
            SubmissionServiceClient submissionServiceClient) {
        this.catalogService = catalogService;
        this.datasetService = datasetService;
        this.problemRepository = problemRepository;
        this.userServiceClient = userServiceClient;
        this.submissionServiceClient = submissionServiceClient;
    }

    @GetMapping
    public CursorPage<ProblemSummaryResponse> listProblems(
            @RequestParam Optional<String> cursor,
            @RequestParam Optional<Integer> limit,
            @RequestParam(name = "q") Optional<String> search,
            @RequestParam Optional<Difficulty> difficulty,
            @RequestParam Optional<EngineType> engine,
            @RequestParam(name = "topic") Optional<String> tag,
            @RequestParam(name = "dataset") Optional<String> datasetSlug,
            @RequestParam Optional<String> status,
            @RequestParam(defaultValue = "false") boolean bookmarked,
            @RequestParam(defaultValue = "recommended") String sort,
            @CurrentUser Optional<AuthenticatedUser> currentUser) {
        PageRequest pageRequest = new PageRequest(limit.orElse(PageRequest.DEFAULT_LIMIT), cursor);

        Optional<Set<String>> slugIn = Optional.empty();
        Optional<Set<String>> slugNotIn = Optional.empty();
        Map<String, String> statuses = Map.of();

        boolean needsStatuses = status.isPresent() || "recommended".equalsIgnoreCase(sort);
        if (needsStatuses && currentUser.isPresent()) {
            statuses = submissionServiceClient.getProblemStatuses(currentUser.get().userId().value());
        }

        if (status.isPresent()) {
            requireAuthenticated(currentUser);
            String normalized = status.get().toUpperCase(Locale.ROOT).replace('-', '_');
            switch (normalized) {
                case "SOLVED", "ATTEMPTED" -> slugIn = Optional.of(intersect(slugIn, slugsWithStatus(normalized, statuses)));
                // "not-started" can't be a slug allowlist (it's every slug NOT in the
                // status map) - it needs slugNotIn, not slugIn, unlike SOLVED/ATTEMPTED.
                case "NOT_STARTED" -> slugNotIn = Optional.of(new LinkedHashSet<>(statuses.keySet()));
                default -> throw new IllegalArgumentException(
                        "status must be one of solved, attempted, not-started - got: " + status.get());
            }
        }

        if (bookmarked) {
            requireAuthenticated(currentUser);
            Set<String> bookmarkedSlugs = new HashSet<>(
                    userServiceClient.getBookmarkedSlugs(currentUser.get().userId().value()));
            slugIn = Optional.of(intersect(slugIn, bookmarkedSlugs));
        }

        if (slugIn.isPresent() && slugIn.get().isEmpty()) {
            return CursorPage.lastPage(List.of());
        }

        ProblemFilter filter = new ProblemFilter(tag, difficulty, engine, search, false, datasetSlug, slugIn, slugNotIn);
        ProblemSort dbSort = resolveSort(sort);
        CursorPage<Problem> page = catalogService.browsePublishedProblems(filter, pageRequest, dbSort);

        if ("recommended".equalsIgnoreCase(sort) && !statuses.isEmpty()) {
            page = reorderUnsolvedFirst(page, statuses);
        }
        return page.map(ProblemSummaryResponse::from);
    }

    @GetMapping("/{slug}")
    public ProblemDetailResponse getProblem(@PathVariable String slug) {
        return ProblemDetailResponse.from(catalogService.getPublishedProblemBySlug(slug));
    }

    @GetMapping("/{slug}/related")
    public List<ProblemSummaryResponse> related(@PathVariable String slug, @RequestParam Optional<Integer> limit) {
        Problem problem = catalogService.getPublishedProblemBySlug(slug);
        List<Problem> candidates = problemRepository.findRelatedCandidates(
                problem.datasetSlug(), problem.tags(), slug, RELATED_CANDIDATE_LIMIT);
        List<Problem> ranked = RelatedProblemsRanker.rank(problem, candidates, limit.orElse(RELATED_DEFAULT_LIMIT));
        return ranked.stream().map(ProblemSummaryResponse::from).toList();
    }

    @GetMapping("/{slug}/schema")
    public DatasetSchemaResponse schema(@PathVariable String slug) {
        Problem problem = catalogService.getPublishedProblemBySlug(slug);
        return DatasetSchemaResponse.from(datasetService.getBySlug(problem.datasetSlug()));
    }

    @GetMapping("/{slug}/sample-data")
    public DatasetSampleDataResponse sampleData(@PathVariable String slug) {
        Problem problem = catalogService.getPublishedProblemBySlug(slug);
        return DatasetSampleDataResponse.from(datasetService.getBySlug(problem.datasetSlug()));
    }

    private static void requireAuthenticated(Optional<AuthenticatedUser> currentUser) {
        if (currentUser.isEmpty()) {
            throw new CurrentUserArgumentResolver.UnauthenticatedException(
                    "The 'status'/'bookmarked' filters require a signed-in caller");
        }
    }

    private static Set<String> slugsWithStatus(String normalizedStatus, Map<String, String> statuses) {
        return statuses.entrySet().stream()
                .filter(e -> normalizedStatus.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> intersect(Optional<Set<String>> existing, Set<String> other) {
        if (existing.isEmpty()) {
            return other;
        }
        Set<String> result = new HashSet<>(existing.get());
        result.retainAll(other);
        return result;
    }

    private static ProblemSort resolveSort(String sort) {
        return switch (sort.toLowerCase(Locale.ROOT)) {
            case "newest" -> ProblemSort.NEWEST_FIRST;
            case "difficulty" -> ProblemSort.DIFFICULTY_THEN_NEWEST;
            // "completion" has no real data source yet (needs submission volume, B09-B11);
            // "recommended" is reordered page-locally below once fetched newest-first.
            case "completion", "recommended" -> ProblemSort.NEWEST_FIRST;
            default -> throw new IllegalArgumentException(
                    "sort must be one of recommended, newest, difficulty, completion - got: " + sort);
        };
    }

    /** Unsolved-first within the fetched page only - see {@link ProblemSort}'s Javadoc for why this can't be a global DB-level sort yet. */
    private static CursorPage<Problem> reorderUnsolvedFirst(CursorPage<Problem> page, Map<String, String> statuses) {
        List<Problem> reordered = new ArrayList<>(page.items());
        reordered.sort((a, b) -> {
            boolean aSolved = "SOLVED".equals(statuses.get(a.slug()));
            boolean bSolved = "SOLVED".equals(statuses.get(b.slug()));
            return Boolean.compare(aSolved, bSolved);
        });
        return new CursorPage<>(reordered, page.nextCursor());
    }
}
