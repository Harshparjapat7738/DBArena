package com.DBArena.services.gamification.repository.badge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.gamification.domain.badge.UserBadge;
import org.bson.Document;

public final class UserBadgeDocumentMapper {

    static final String ID = "_id";
    static final String USER_ID = "userId";
    static final String BADGE_SLUG = "badgeSlug";
    static final String EARNED_AT = "earnedAt";

    private UserBadgeDocumentMapper() {
    }

    public static Document toDocument(UserBadge badge) {
        return new Document()
                .append(ID, badge.id().value())
                .append(USER_ID, badge.userId().value())
                .append(BADGE_SLUG, badge.badgeSlug())
                .append(EARNED_AT, badge.earnedAtEpochMillis());
    }

    public static UserBadge fromDocument(Document document) {
        return new UserBadge(
                TypedId.of(document.getString(ID)),
                TypedId.of(document.getString(USER_ID)),
                document.getString(BADGE_SLUG),
                document.getLong(EARNED_AT));
    }
}
