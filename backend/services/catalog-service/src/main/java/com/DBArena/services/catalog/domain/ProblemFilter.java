package com.DBArena.services.catalog.domain;

import com.DBArena.engine.spi.EngineType;

import java.util.Optional;
import java.util.Set;

/**
 * Every field is optional-and-composable; an absent field applies no filter
 * on that axis. {@code slugIn}/{@code slugNotIn} (B03) are the join point for
 * filters this service can't evaluate itself - "bookmarked" (user-service)
 * and "status" (submission-service) are resolved to a slug set by the
 * caller (see {@code ProblemsController}) and passed in here as an ordinary
 * Mongo {@code $in}/{@code $nin}, exactly like any other clause.
 */
public record ProblemFilter(
        Optional<String> tag,
        Optional<Difficulty> difficulty,
        Optional<EngineType> engine,
        Optional<String> titleSearch,
        boolean publishedOnly,
        Optional<String> datasetSlug,
        Optional<Set<String>> slugIn,
        Optional<Set<String>> slugNotIn) {

    public ProblemFilter {
        tag = tag == null ? Optional.empty() : tag;
        difficulty = difficulty == null ? Optional.empty() : difficulty;
        engine = engine == null ? Optional.empty() : engine;
        titleSearch = titleSearch == null ? Optional.empty() : titleSearch.filter(s -> !s.isBlank());
        datasetSlug = datasetSlug == null ? Optional.empty() : datasetSlug;
        slugIn = slugIn == null ? Optional.empty() : slugIn.map(Set::copyOf);
        slugNotIn = slugNotIn == null ? Optional.empty() : slugNotIn.map(Set::copyOf);
    }

    /**
     * Back-compat convenience constructor (B03) - every call site written
     * before {@code datasetSlug}/{@code slugIn}/{@code slugNotIn} existed
     * passes exactly these 5 args; defaults the three new fields to absent
     * rather than forcing every caller to learn about them.
     */
    public ProblemFilter(
            Optional<String> tag,
            Optional<Difficulty> difficulty,
            Optional<EngineType> engine,
            Optional<String> titleSearch,
            boolean publishedOnly) {
        this(tag, difficulty, engine, titleSearch, publishedOnly, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** The public-browsing default: published problems only, no other filter applied. */
    public static ProblemFilter onlyPublished() {
        return new ProblemFilter(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), true);
    }
}
