package com.DBArena.services.gamification.domain.dailychallenge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;

/**
 * One calendar day's featured problem. {@code date} is an ISO-8601
 * {@code yyyy-MM-dd} string (UTC calendar day), unique - see the Mongock
 * changelog - so there is exactly one challenge per day, never a BSON Date
 * (hard rule #9 is about timestamps; a calendar day has no time component
 * to begin with, so a plain date string is the right representation, not a
 * rule exception).
 */
public record DailyChallenge(
        TypedId<DailyChallenge> id,
        String date,
        String problemSlug,
        EngineType engine,
        String topic,
        int estimatedMinutes,
        int xpReward) {

    public DailyChallenge {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date must not be blank");
        }
        if (problemSlug == null || problemSlug.isBlank()) {
            throw new IllegalArgumentException("problemSlug must not be blank");
        }
    }
}
