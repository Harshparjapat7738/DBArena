package com.DBArena.services.gamification.repository.leaderboard;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.leaderboard.LeaderboardEntry;
import org.bson.Document;

public final class LeaderboardEntryDocumentMapper {

    static final String ID = "_id";
    static final String SCOPE = "scope";
    static final String PERIOD_KEY = "periodKey";
    static final String USER_ID = "userId";
    static final String RANK = "rank";
    static final String XP = "xp";
    static final String SOLVED = "solved";
    static final String STREAK = "streak";
    static final String COMPUTED_AT = "computedAt";

    private LeaderboardEntryDocumentMapper() {
    }

    public static Document toDocument(LeaderboardEntry entry) {
        return new Document()
                .append(ID, entry.id().value())
                .append(SCOPE, entry.scope())
                .append(PERIOD_KEY, entry.periodKey())
                .append(USER_ID, entry.userId().value())
                .append(RANK, entry.rank())
                .append(XP, entry.xp())
                .append(SOLVED, entry.solved())
                .append(STREAK, entry.streak())
                .append(COMPUTED_AT, entry.computedAtEpochMillis());
    }

    public static LeaderboardEntry fromDocument(Document document) {
        return new LeaderboardEntry(
                TypedId.of(document.getString(ID)),
                document.getString(SCOPE),
                document.getString(PERIOD_KEY),
                TypedId.<AuthenticatedUser>of(document.getString(USER_ID)),
                document.getInteger(RANK, 0),
                document.getLong(XP),
                document.getInteger(SOLVED, 0),
                document.getInteger(STREAK, 0),
                document.getLong(COMPUTED_AT));
    }
}
