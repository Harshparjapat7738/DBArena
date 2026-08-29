package com.dbforge.services.catalog.web.dto;

import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.engine.spi.EngineType;
import com.dbforge.services.catalog.service.CreateProblemCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record CreateProblemRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "must be lowercase-kebab-case") String slug,
        @NotBlank String title,
        @NotBlank String statementMarkdown,
        @NotNull Difficulty difficulty,
        Set<String> tags,
        @NotEmpty Set<EngineType> allowedEngines,
        String datasetSlug) {

    public CreateProblemRequest {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public CreateProblemCommand toCommand() {
        return new CreateProblemCommand(slug, title, statementMarkdown, difficulty, tags, allowedEngines, datasetSlug);
    }
}
