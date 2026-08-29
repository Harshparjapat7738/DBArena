package com.DBArena.services.gamification.repository.dailychallenge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.dailychallenge.DailyChallengeCompletion;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import static com.DBArena.services.gamification.repository.dailychallenge.DailyChallengeCompletionDocumentMapper.*;

@Repository
public class MongoDailyChallengeCompletionRepository implements DailyChallengeCompletionRepository {

    private final MongoCollection<Document> collection;

    public MongoDailyChallengeCompletionRepository(MongoCollection<Document> dailyChallengeCompletionsCollection) {
        this.collection = dailyChallengeCompletionsCollection;
    }

    @Override
    public void insert(DailyChallengeCompletion completion) {
        collection.insertOne(toDocument(completion));
    }

    @Override
    public boolean existsByUserIdAndDate(TypedId<AuthenticatedUser> userId, String date) {
        return collection.countDocuments(Filters.and(Filters.eq(USER_ID, userId.value()), Filters.eq(DATE, date))) > 0;
    }
}
