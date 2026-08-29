package com.DBArena.services.gamification.repository.progress;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.progress.UserProgress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.DBArena.services.gamification.repository.progress.UserProgressDocumentMapper.*;

@Repository
public class MongoUserProgressRepository implements UserProgressRepository {

    private final MongoCollection<Document> collection;

    public MongoUserProgressRepository(MongoCollection<Document> userProgressCollection) {
        this.collection = userProgressCollection;
    }

    @Override
    public Optional<UserProgress> findByUserId(TypedId<AuthenticatedUser> userId) {
        Document document = collection.find(Filters.eq(ID, userId.value())).first();
        return Optional.ofNullable(document).map(UserProgressDocumentMapper::fromDocument);
    }

    @Override
    public void upsert(UserProgress progress) {
        collection.replaceOne(
                Filters.eq(ID, progress.userId().value()),
                toDocument(progress),
                new ReplaceOptions().upsert(true));
    }
}
