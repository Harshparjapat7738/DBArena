package com.DBArena.services.gamification.repository.dailychallenge;

import com.DBArena.services.gamification.domain.dailychallenge.DailyChallenge;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.DBArena.services.gamification.repository.dailychallenge.DailyChallengeDocumentMapper.*;

@Repository
public class MongoDailyChallengeRepository implements DailyChallengeRepository {

    private final MongoCollection<Document> collection;

    public MongoDailyChallengeRepository(MongoCollection<Document> dailyChallengesCollection) {
        this.collection = dailyChallengesCollection;
    }

    @Override
    public void insert(DailyChallenge challenge) {
        collection.insertOne(toDocument(challenge));
    }

    @Override
    public Optional<DailyChallenge> findByDate(String date) {
        Document document = collection.find(Filters.eq(DATE, date)).first();
        return Optional.ofNullable(document).map(DailyChallengeDocumentMapper::fromDocument);
    }

    @Override
    public List<DailyChallenge> findRecent(int limit) {
        List<DailyChallenge> result = new ArrayList<>();
        for (Document document : collection.find().sort(Sorts.descending(DATE)).limit(limit)) {
            result.add(DailyChallengeDocumentMapper.fromDocument(document));
        }
        return result;
    }
}
