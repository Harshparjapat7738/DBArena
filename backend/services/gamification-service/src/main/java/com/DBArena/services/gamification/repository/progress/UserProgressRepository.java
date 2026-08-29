package com.DBArena.services.gamification.repository.progress;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.progress.UserProgress;

import java.util.Optional;

public interface UserProgressRepository {

    Optional<UserProgress> findByUserId(TypedId<AuthenticatedUser> userId);

    /** Full-document upsert, keyed on userId - see {@link UserProgress}'s Javadoc for why. */
    void upsert(UserProgress progress);
}
