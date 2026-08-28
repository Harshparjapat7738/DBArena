package com.dbforge.services.catalog.service;

import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.services.catalog.domain.EngineKind;

import java.util.Set;

public record UpdateProblemCommand(
        String title,
        String statementMarkdown,
        Difficulty difficulty,
        Set<String> tags,
        Set<EngineKind> allowedEngines,
        String datasetSlug) {
}
