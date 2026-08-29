package com.DBArena.services.catalog.web;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.dataset.DatasetFilter;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;
import com.DBArena.services.catalog.service.DatasetService;
import com.DBArena.services.catalog.web.dto.dataset.DatasetResponse;
import com.DBArena.services.catalog.web.dto.dataset.DatasetSampleDataResponse;
import com.DBArena.services.catalog.web.dto.dataset.DatasetSchemaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * B03. All-public GET surface, same posture as {@link ProblemController}'s
 * browsing endpoints (api-gateway's {@code PublicPaths} allowlists GET on
 * this prefix - see the B03 Session Log entry) - dataset schema/sample data
 * is reference material, not user- or admin-scoped, so nothing here needs
 * {@code @CurrentUser}.
 */
@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping
    public CursorPage<DatasetResponse> listDatasets(
            @RequestParam Optional<String> cursor,
            @RequestParam Optional<Integer> limit,
            @RequestParam Optional<String> category,
            @RequestParam Optional<EngineType> engine,
            @RequestParam(name = "q") Optional<String> search) {
        PageRequest pageRequest = new PageRequest(limit.orElse(PageRequest.DEFAULT_LIMIT), cursor);
        DatasetFilter filter = new DatasetFilter(category, engine, search);
        return datasetService.browse(filter, pageRequest)
                .map(dataset -> DatasetResponse.forListing(dataset, datasetService.problemCount(dataset.slug())));
    }

    @GetMapping("/{slug}")
    public DatasetResponse getDataset(@PathVariable String slug) {
        DatasetMetadata dataset = datasetService.getBySlug(slug);
        return DatasetResponse.full(dataset, datasetService.problemCount(slug));
    }

    @GetMapping("/{slug}/schema")
    public DatasetSchemaResponse getSchema(@PathVariable String slug) {
        return DatasetSchemaResponse.from(datasetService.getBySlug(slug));
    }

    @GetMapping("/{slug}/sample-data")
    public DatasetSampleDataResponse getSampleData(@PathVariable String slug) {
        return DatasetSampleDataResponse.from(datasetService.getBySlug(slug));
    }

    @GetMapping("/{slug}/engines")
    public List<EngineType> getEngines(@PathVariable String slug) {
        return List.copyOf(datasetService.getBySlug(slug).engines());
    }
}
