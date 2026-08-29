package com.DBArena.services.gamification.repository.leaderboard;

import com.DBArena.services.gamification.domain.leaderboard.LeaderboardEntry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.DBArena.services.gamification.repository.leaderboard.LeaderboardEntryDocumentMapper.*;

@Repository
public class MongoLeaderboardRepository implements LeaderboardRepository {

    private final MongoCollection<Document> collection;

    public MongoLeaderboardRepository(MongoCollection<Document> leaderboardEntriesCollection) {
        this.collection = leaderboardEntriesCollection;
    }

    @Override
    public List<LeaderboardEntry> findTop(String scope, String periodKey, int limit) {
        Bson query = Filters.and(Filters.eq(SCOPE, scope), Filters.eq(PERIOD_KEY, periodKey));
        List<LeaderboardEntry> result = new ArrayList<>();
        for (Document document : collection.find(query).sort(Sorts.ascending(RANK)).limit(limit)) {
            result.add(LeaderboardEntryDocumentMapper.fromDocument(document));
        }
        return result;
    }

    @Override
    public void replaceSnapshot(String scope, String periodKey, List<LeaderboardEntry> entries) {
        collection.deleteMany(Filters.and(Filters.eq(SCOPE, scope), Filters.eq(PERIOD_KEY, periodKey)));
        if (!entries.isEmpty()) {
            collection.insertMany(entries.stream().map(LeaderboardEntryDocumentMapper::toDocument).toList());
        }
    }
}
