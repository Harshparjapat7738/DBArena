package com.DBArena.services.gamification.domain.activity;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;

import java.util.Optional;

/**
 * One entry in a user's activity feed. Kept as its own collection rather
 * than an array embedded in {@link com.DBArena.services.gamification.domain.progress.UserProgress}
 * deliberately - an activity feed grows without bound over a user's
 * lifetime, and B02's brief calls out "high-volume ... compatibility";
 * embedding would eventually blow past MongoDB's 16MB document limit.
 */
public record ActivityItem(
        TypedId<ActivityItem> id,
        TypedId<AuthenticatedUser> userId,
        String type,
        String message,
        Optional<String> refSlug,
        long occurredAtEpochMillis) {

    public ActivityItem {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        refSlug = refSlug == null ? Optional.empty() : refSlug;
    }
}
