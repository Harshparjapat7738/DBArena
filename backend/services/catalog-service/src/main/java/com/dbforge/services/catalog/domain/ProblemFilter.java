package com.dbforge.services.catalog.domain;

import java.util.Optional;

/** Every field is optional-and-composable; an absent field applies no filter on that axis. */
public record ProblemFilter(
        Optional<String> tag,
        Optional<Difficulty> difficulty,
        Optional<EngineKind> engine,
        Optional<String> titleSearch,
        boolean publishedOnly) {

    public ProblemFilter {
        tag = tag == null ? Optional.empty() : tag;
        difficulty = difficulty == null ? Optional.empty() : difficulty;
        engine = engine == null ? Optional.empty() : engine;
        titleSearch = titleSearch == null ? Optional.empty() : titleSearch.filter(s -> !s.isBlank());
    }

    /** The public-browsing default: published problems only, no other filter applied. */
    public static ProblemFilter publishedOnly() {
        return new ProblemFilter(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), true);
    }
}
