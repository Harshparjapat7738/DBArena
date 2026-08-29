package com.DBArena.services.ai.web.dto;

import com.DBArena.services.ai.domain.HintCommand;
import com.DBArena.services.ai.domain.HintLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Optional;

public record HintRequestBody(
        @NotBlank @Size(max = 8000) String learnerQuery,
        @Size(max = 4000) String errorOrResultText,
        @NotNull HintLevel level) {

    public HintCommand toCommand(String problemSlug) {
        return new HintCommand(problemSlug, learnerQuery, Optional.ofNullable(errorOrResultText), level);
    }
}
