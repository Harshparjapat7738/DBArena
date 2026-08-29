package com.DBArena.services.gamification.repository.badge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.badge.UserBadge;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.DBArena.services.gamification.repository.badge.UserBadgeDocumentMapper.*;

@Repository
public class MongoUserBadgeRepository implements UserBadgeRepository {

    private final MongoCollection<Document> collection;

    public MongoUserBadgeRepository(MongoCollection<Document> userBadgesCollection) {
        this.collection = userBadgesCollection;
    }

    @Override
    public void insert(UserBadge badge) {
        collection.insertOne(toDocument(badge));
    }

    @Override
    public boolean existsByUserIdAndBadgeSlug(TypedId<AuthenticatedUser> userId, String badgeSlug) {
        return collection.countDocuments(
                Filters.and(Filters.eq(USER_ID, userId.value()), Filters.eq(BADGE_SLUG, badgeSlug))) > 0;
    }

    @Override
    public List<UserBadge> findByUserId(TypedId<AuthenticatedUser> userId) {
        List<UserBadge> result = new ArrayList<>();
        for (Document document : collection.find(Filters.eq(USER_ID, userId.value()))) {
            result.add(UserBadgeDocumentMapper.fromDocument(document));
        }
        return result;
    }
}
