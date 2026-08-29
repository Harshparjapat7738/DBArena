package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.engine.spi.EngineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;

public record CreateDatasetMetadataRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "must be lowercase-kebab-case") String slug,
        @NotBlank String name,
        String description,
        String category,
        @NotEmpty Set<EngineType> engines,
        @PositiveOrZero int entityCount,
        String rowCountLabel) {

    public CreateDatasetMetadataRequest {
        description = description == null ? "" : description;
        category = category == null ? "uncategorized" : category;
        rowCountLabel = rowCountLabel == null ? "" : rowCountLabel;
    }
}
