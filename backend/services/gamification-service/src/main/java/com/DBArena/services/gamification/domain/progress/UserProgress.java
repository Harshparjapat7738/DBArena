package com.DBArena.services.gamification.domain.progress;

import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.core.id.TypedId;

import java.util.List;
import java.util.Optional;

/**
 * Exactly one document per user, keyed on {@code userId} itself (not a
 * separately-generated id) - progress is updated far more often than it is
 * created (every accepted submission, once B11 exists), so an upsert by a
 * natural key beats insert-then-look-up-by-generated-id for this access
 * pattern. This is the one departure from every other B02 collection's
 * ULID-primary-key convention, and is deliberate: see repository Javadoc.
 */
public record UserProgress(
        TypedId<AuthenticatedUser> userId,
        long xp,
        int level,
        long xpIntoLevel,
        long xpForNextLevel,
        int streakCurrent,
        int streakLongest,
        Optional<String> streakLastActiveDate,
        int freezesAvailable,
        List<SkillMastery> masteries,
        long updatedAtEpochMillis) {

    public UserProgress {
        masteries = List.copyOf(masteries);
        streakLastActiveDate = streakLastActiveDate == null ? Optional.empty() : streakLastActiveDate;
        if (xp < 0) {
            throw new IllegalArgumentException("xp must not be negative");
        }
    }

    public static UserProgress initial(TypedId<AuthenticatedUser> userId, long nowEpochMillis) {
        return new UserProgress(userId, 0L, 1, 0L, 100L, 0, 0, Optional.empty(), 0, List.of(), nowEpochMillis);
    }
}
