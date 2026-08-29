package com.DBArena.services.catalog.service;

import com.DBArena.services.catalog.domain.Problem;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, dependency-free (no Mongo, no Spring) so it's unit-testable without
 * a running database - same convention as the repository document mappers.
 * Mirrors the frontend mock's {@code problemsRepository.getRelated}
 * heuristic exactly: +2 for sharing a dataset, +1 per shared tag, keep only
 * a positive score, highest first.
 */
public final class RelatedProblemsRanker {

    private RelatedProblemsRanker() {
    }

    public static List<Problem> rank(Problem subject, List<Problem> candidates, int limit) {
        record Scored(Problem problem, int score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Problem candidate : candidates) {
            int score = 0;
            if (!subject.datasetSlug().isBlank() && subject.datasetSlug().equals(candidate.datasetSlug())) {
                score += 2;
            }
            for (String tag : candidate.tags()) {
                if (subject.tags().contains(tag)) {
                    score += 1;
                }
            }
            if (score > 0) {
                scored.add(new Scored(candidate, score));
            }
        }
        scored.sort((a, b) -> b.score() - a.score());
        return scored.stream().limit(limit).map(Scored::problem).toList();
    }
}
