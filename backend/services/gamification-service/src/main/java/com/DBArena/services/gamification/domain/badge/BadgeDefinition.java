package com.DBArena.services.gamification.domain.badge;

import com.DBArena.common.core.id.TypedId;

/** The static badge catalog - admin-managed content, not per-user state. See {@link UserBadge} for who has earned what. */
public record BadgeDefinition(
        TypedId<BadgeDefinition> id,
        String slug,
        String name,
        String description,
        String icon,
        BadgeTier tier) {

    public BadgeDefinition {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
    }
}
