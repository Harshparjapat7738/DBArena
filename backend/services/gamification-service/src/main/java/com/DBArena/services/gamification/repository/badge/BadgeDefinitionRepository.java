package com.DBArena.services.gamification.repository.badge;

import com.DBArena.services.gamification.domain.badge.BadgeDefinition;

import java.util.List;
import java.util.Optional;

public interface BadgeDefinitionRepository {

    void insert(BadgeDefinition badge);

    Optional<BadgeDefinition> findBySlug(String slug);

    /** The full catalog - small and admin-curated, no pagination needed (unlike user-scoped, ever-growing collections). */
    List<BadgeDefinition> findAll();
}
