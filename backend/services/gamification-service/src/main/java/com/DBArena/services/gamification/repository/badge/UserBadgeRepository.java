package com.DBArena.services.gamification.repository.badge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.badge.UserBadge;

import java.util.List;

public interface UserBadgeRepository {

    void insert(UserBadge badge);

    boolean existsByUserIdAndBadgeSlug(TypedId<AuthenticatedUser> userId, String badgeSlug);

    List<UserBadge> findByUserId(TypedId<AuthenticatedUser> userId);
}
