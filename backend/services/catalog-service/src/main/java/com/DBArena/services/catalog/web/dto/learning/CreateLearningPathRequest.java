package com.DBArena.services.catalog.web.dto.learning;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.learning.LearningLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record CreateLearningPathRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "must be lowercase-kebab-case") String slug,
        @NotBlank String title,
        String description,
        @NotNull LearningLevel level,
        @NotNull EngineType engine,
        @NotEmpty List<LessonRequest> lessons,
        @PositiveOrZero int estimatedHours) {

    public CreateLearningPathRequest {
        description = description == null ? "" : description;
        lessons = List.copyOf(lessons);
    }
}
