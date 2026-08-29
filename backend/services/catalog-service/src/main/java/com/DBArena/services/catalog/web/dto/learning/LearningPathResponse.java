package com.DBArena.services.catalog.web.dto.learning;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.learning.LearningLevel;
import com.DBArena.services.catalog.domain.learning.LearningPath;

import java.util.List;

public record LearningPathResponse(
        String slug,
        String title,
        String description,
        LearningLevel level,
        EngineType engine,
        List<LessonResponse> lessons,
        int estimatedHours,
        int version) {

    public static LearningPathResponse from(LearningPath path) {
        return new LearningPathResponse(path.slug(), path.title(), path.description(), path.level(), path.engine(),
                path.lessons().stream().map(LessonResponse::from).toList(), path.estimatedHours(), path.version());
    }
}
