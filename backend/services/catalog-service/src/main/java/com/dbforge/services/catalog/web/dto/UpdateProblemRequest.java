package com.dbforge.services.catalog.web.dto;

import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.services.catalog.domain.EngineKind;
import com.dbforge.services.catalog.service.UpdateProblemCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateProblemRequest(
        @NotBlank String title,
        @NotBlank String statementMarkdown,
        @NotNull Difficulty difficulty,
        Set<String> tags,
        @NotEmpty Set<EngineKind> allowedEngines,
        String datasetSlug) {

    public UpdateProblemRequest {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public UpdateProblemCommand toCommand() {
        return new UpdateProblemCommand(title, statementMarkdown, difficulty, tags, allowedEngines, datasetSlug);
    }
}
