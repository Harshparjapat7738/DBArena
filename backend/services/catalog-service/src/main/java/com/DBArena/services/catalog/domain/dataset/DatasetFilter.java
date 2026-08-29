package com.DBArena.services.catalog.domain.dataset;

import com.DBArena.engine.spi.EngineType;

import java.util.Optional;

/** B03: filters for {@code GET /api/v1/datasets} - every field optional-and-composable, same convention as {@code ProblemFilter}. */
public record DatasetFilter(Optional<String> category, Optional<EngineType> engine, Optional<String> nameSearch) {

    public DatasetFilter {
        category = category == null ? Optional.empty() : category;
        engine = engine == null ? Optional.empty() : engine;
        nameSearch = nameSearch == null ? Optional.empty() : nameSearch.filter(s -> !s.isBlank());
    }

    public static DatasetFilter none() {
        return new DatasetFilter(Optional.empty(), Optional.empty(), Optional.empty());
    }
}
