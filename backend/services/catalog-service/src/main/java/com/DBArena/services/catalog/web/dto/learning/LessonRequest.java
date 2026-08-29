package com.DBArena.services.catalog.web.dto.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record LessonRequest(
        @NotBlank String slug,
        @NotBlank String title,
        String summary,
        @PositiveOrZero int durationMinutes,
        @PositiveOrZero int order,
        String practiceProblemSlug) {

    public LessonRequest {
        summary = summary == null ? "" : summary;
    }
}
