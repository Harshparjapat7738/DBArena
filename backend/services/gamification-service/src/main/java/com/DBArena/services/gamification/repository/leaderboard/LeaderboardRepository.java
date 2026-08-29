package com.DBArena.services.gamification.repository.leaderboard;

import com.DBArena.services.gamification.domain.leaderboard.LeaderboardEntry;

import java.util.List;

public interface LeaderboardRepository {

    /** Top-N rows for one (scope, periodKey) snapshot, ordered by rank ascending. */
    List<LeaderboardEntry> findTop(String scope, String periodKey, int limit);

    /**
     * Atomically replaces every row of one (scope, periodKey) snapshot -
     * the ranking job (not part of B02) always recomputes a full snapshot
     * rather than patching individual rows, so a stale rank never survives
     * a recompute.
     */
    void replaceSnapshot(String scope, String periodKey, List<LeaderboardEntry> entries);
}
