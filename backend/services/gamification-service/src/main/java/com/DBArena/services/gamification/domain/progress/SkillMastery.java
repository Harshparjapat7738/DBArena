package com.DBArena.services.gamification.domain.progress;

/**
 * Embedded within {@link UserProgress}, one entry per topic a user has
 * touched - bounded by the number of {@code catalog-service} topics
 * (small, admin-curated), never by submission volume, so embedding here
 * doesn't risk unbounded document growth the way a raw activity/submission
 * log would.
 */
public record SkillMastery(String topic, int masteryPct, int problemsSolved, int problemsTotal) {

    public SkillMastery {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        if (masteryPct < 0 || masteryPct > 100) {
            throw new IllegalArgumentException("masteryPct must be between 0 and 100");
        }
    }
}
