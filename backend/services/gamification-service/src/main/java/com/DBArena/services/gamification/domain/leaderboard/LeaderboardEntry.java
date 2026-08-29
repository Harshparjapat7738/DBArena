package com.DBArena.services.gamification.domain.leaderboard;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;

/**
 * One row of a materialized leaderboard snapshot. {@code scope} is a plain
 * string rather than a closed enum - the frontend's own scope union
 * ({@code "global"|"weekly"|"monthly"|EngineKind}, per B01's audit) mixes
 * time-window scopes with per-engine scopes, so a single enum would have to
 * either duplicate {@code EngineType} or grow every time a new time window
 * is added; a validated string keeps this collection's shape decoupled from
 * both.
 *
 * <p>Rows are written by a periodic ranking job (not part of B02 - that job
 * doesn't exist until real submissions exist to rank), never computed live
 * off a full collection scan - {@code periodKey} disambiguates snapshots
 * across time windows (e.g. {@code "ALL"}, {@code "2026-W35"}, {@code "2026-08"}).
 */
public record LeaderboardEntry(
        TypedId<LeaderboardEntry> id,
        String scope,
        String periodKey,
        TypedId<AuthenticatedUser> userId,
        int rank,
        long xp,
        int solved,
        int streak,
        long computedAtEpochMillis) {

    public LeaderboardEntry {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }
        if (periodKey == null || periodKey.isBlank()) {
            throw new IllegalArgumentException("periodKey must not be blank");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("rank must be >= 1");
        }
    }
}
