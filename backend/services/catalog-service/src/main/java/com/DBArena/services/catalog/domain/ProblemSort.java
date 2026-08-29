package com.DBArena.services.catalog.domain;

/**
 * DB-level sort modes for {@link com.DBArena.services.catalog.repository.ProblemRepository#findPage}.
 * The frontend's own sort modes ({@code recommended}/{@code newest}/
 * {@code difficulty}/{@code completion}, per the mock {@code PracticeRefinements})
 * map onto this smaller set - {@code recommended} and {@code completion} have
 * no real data source yet (they need per-user solve state and submission
 * volume respectively, neither of which exists before B09-B11), so
 * {@code ProblemsController} maps both to {@link #NEWEST_FIRST} for now and
 * says so in its own Javadoc, rather than silently pretending the ranking is
 * real.
 */
public enum ProblemSort {
    /** Original catalog-browsing order (oldest created first) - what {@code /api/v1/catalog/problems} has always returned; left unchanged so that endpoint's cursor format never changes. */
    OLDEST_FIRST,
    NEWEST_FIRST,
    DIFFICULTY_THEN_NEWEST
}
