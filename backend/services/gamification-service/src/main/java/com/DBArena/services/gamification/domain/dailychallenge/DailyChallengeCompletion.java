package com.DBArena.services.gamification.domain.dailychallenge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;

/** One user's completion of one day's {@link DailyChallenge}. (userId, date) is unique - a day can only be completed once. */
public record DailyChallengeCompletion(
        TypedId<DailyChallengeCompletion> id,
        TypedId<AuthenticatedUser> userId,
        String date,
        long completedAtEpochMillis,
        int xpAwarded) {

    public DailyChallengeCompletion {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date must not be blank");
        }
    }
}
