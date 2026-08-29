package com.DBArena.services.gamification.repository.dailychallenge;

import com.DBArena.services.gamification.domain.dailychallenge.DailyChallenge;

import java.util.List;
import java.util.Optional;

public interface DailyChallengeRepository {

    void insert(DailyChallenge challenge);

    Optional<DailyChallenge> findByDate(String date);

    /** Most recent first, capped by the caller - history is small (one row/day), no cursor pagination needed yet. */
    List<DailyChallenge> findRecent(int limit);
}
