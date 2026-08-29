package com.DBArena.services.gamification.repository.dailychallenge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.dailychallenge.DailyChallengeCompletion;

public interface DailyChallengeCompletionRepository {

    void insert(DailyChallengeCompletion completion);

    boolean existsByUserIdAndDate(TypedId<AuthenticatedUser> userId, String date);
}
