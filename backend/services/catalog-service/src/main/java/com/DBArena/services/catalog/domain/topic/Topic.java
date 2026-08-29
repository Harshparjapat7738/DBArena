package com.DBArena.services.catalog.domain.topic;

import com.DBArena.common.core.id.TypedId;

/**
 * A canonical topic/skill tag (B02) - distinct from {@code Problem.tags()},
 * which stays a free-form {@code Set<String>} authors type by hand. This is
 * the registry those tag strings are meant to eventually be validated
 * against, and what learning paths and skill-mastery tracking (gamification-
 * service) key off of, so "arrays" said one way in a problem and another way
 * in a learning path don't silently diverge. Nothing enforces that
 * cross-reference yet in B02 - see backend/CLAUDE.md's B02 Session Log entry.
 */
public record Topic(
        TypedId<Topic> id,
        String slug,
        String name,
        String description,
        long createdAtEpochMillis) {

    public Topic {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
