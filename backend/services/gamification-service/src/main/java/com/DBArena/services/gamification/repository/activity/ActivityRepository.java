package com.DBArena.services.gamification.repository.activity;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.activity.ActivityItem;

public interface ActivityRepository {

    void insert(ActivityItem item);

    CursorPage<ActivityItem> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest);
}
