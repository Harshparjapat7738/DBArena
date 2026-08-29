package com.DBArena.services.catalog.web;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.rbac.RequiresRole;
import com.DBArena.services.catalog.domain.Difficulty;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.ProblemFilter;
import com.DBArena.services.catalog.domain.TagCount;
import com.DBArena.services.catalog.service.CatalogService;
import com.DBArena.services.catalog.web.dto.CreateProblemRequest;
import com.DBArena.services.catalog.web.dto.ProblemDetailResponse;
import com.DBArena.services.catalog.web.dto.ProblemSummaryResponse;
import com.DBArena.services.catalog.web.dto.UpdateProblemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Browsing (the {@code GET}s) is public - api-gateway's {@code PublicPaths}
 * allowlists them, and this controller itself needs no {@code @CurrentUser}
 * to serve them. Authoring ({@code POST}/{@code PUT}) requires the
 * {@code admin} role via {@code @RequiresRole}, enforced here regardless of
 * what the gateway does - the gateway's allow/deny is a convenience, not
 * the boundary (see api-gateway's GatewayAccessFilter Javadoc).
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class ProblemController {

    private final CatalogService catalogService;

    public ProblemController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/problems")
    public CursorPage<ProblemSummaryResponse> listProblems(
            @RequestParam Optional<String> cursor,
            @RequestParam Optional<Integer> limit,
            @RequestParam Optional<String> tag,
            @RequestParam Optional<Difficulty> difficulty,
            @RequestParam Optional<EngineType> engine,
            @RequestParam(name = "q") Optional<String> search) {
        PageRequest pageRequest = new PageRequest(limit.orElse(PageRequest.DEFAULT_LIMIT), cursor);
        // publishedOnly is forced true inside the service regardless of what's passed here.
        ProblemFilter filter = new ProblemFilter(tag, difficulty, engine, search, false);
        return catalogService.browsePublishedProblems(filter, pageRequest).map(ProblemSummaryResponse::from);
    }

    @GetMapping("/problems/{slug}")
    public ProblemDetailResponse getProblem(@PathVariable String slug) {
        return ProblemDetailResponse.from(catalogService.getPublishedProblemBySlug(slug));
    }

    @GetMapping("/tags")
    public List<TagCount> listTags() {
        return catalogService.listPublishedTagCounts();
    }

    @PostMapping("/problems")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole("admin")
    public ProblemDetailResponse createProblem(@Valid @RequestBody CreateProblemRequest request) {
        return ProblemDetailResponse.from(catalogService.createProblem(request.toCommand()));
    }

    @PutMapping("/problems/{slug}")
    @RequiresRole("admin")
    public ProblemDetailResponse updateProblem(
            @PathVariable String slug, @Valid @RequestBody UpdateProblemRequest request) {
        return ProblemDetailResponse.from(catalogService.updateProblem(slug, request.toCommand()));
    }

    @PostMapping("/problems/{slug}/publish")
    @RequiresRole("admin")
    public ProblemDetailResponse publish(@PathVariable String slug) {
        return ProblemDetailResponse.from(catalogService.setPublished(slug, true));
    }

    @PostMapping("/problems/{slug}/unpublish")
    @RequiresRole("admin")
    public ProblemDetailResponse unpublish(@PathVariable String slug) {
        return ProblemDetailResponse.from(catalogService.setPublished(slug, false));
    }
}
