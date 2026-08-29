package com.DBArena.services.gamification.repository.leaderboard;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.gamification.domain.leaderboard.LeaderboardEntry;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardEntryDocumentMapperTest {

    @Test
    void roundTripsEveryField() {
        LeaderboardEntry entry = new LeaderboardEntry(
                TypedId.of("01J000ENTRY"), "GLOBAL", "ALL", TypedId.of("01J000USER"), 1, 9800L, 42, 15, 1_700_000_000_000L);

        Document document = LeaderboardEntryDocumentMapper.toDocument(entry);
        LeaderboardEntry roundTripped = LeaderboardEntryDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(entry);
    }
}
