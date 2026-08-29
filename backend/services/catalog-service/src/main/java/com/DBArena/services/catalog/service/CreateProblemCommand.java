package com.DBArena.services.catalog.service;

import com.DBArena.services.catalog.domain.Difficulty;
import com.DBArena.engine.spi.EngineType;

import java.util.Set;

public record CreateProblemCommand(
        String slug,
        String title,
        String statementMarkdown,
        Difficulty difficulty,
        Set<String> tags,
        Set<EngineType> allowedEngines,
        String datasetSlug) {
}
