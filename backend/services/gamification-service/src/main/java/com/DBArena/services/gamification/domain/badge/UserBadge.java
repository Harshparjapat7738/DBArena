package com.DBArena.services.gamification.domain.badge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;

/** One award of a {@link BadgeDefinition} to a user. A (userId, badgeSlug) pair is unique - see the Mongock changelog. */
public record UserBadge(
        TypedId<UserBadge> id,
        TypedId<AuthenticatedUser> userId,
        String badgeSlug,
        long earnedAtEpochMillis) {

    public UserBadge {
        if (badgeSlug == null || badgeSlug.isBlank()) {
            throw new IllegalArgumentException("badgeSlug must not be blank");
        }
    }
}
