package com.dbforge.services.catalog.service;

import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.engine.spi.EngineType;

import java.util.Set;

public record UpdateProblemCommand(
        String title,
        String statementMarkdown,
        Difficulty difficulty,
        Set<String> tags,
        Set<EngineType> allowedEngines,
        String datasetSlug) {
}
