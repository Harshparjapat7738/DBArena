package com.DBArena.services.gamification.repository.activity;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.activity.ActivityItem;
import org.bson.Document;

import java.util.Optional;

public final class ActivityItemDocumentMapper {

    static final String ID = "_id";
    static final String USER_ID = "userId";
    static final String TYPE = "type";
    static final String MESSAGE = "message";
    static final String REF_SLUG = "refSlug";
    static final String OCCURRED_AT = "occurredAt";

    private ActivityItemDocumentMapper() {
    }

    public static Document toDocument(ActivityItem item) {
        return new Document()
                .append(ID, item.id().value())
                .append(USER_ID, item.userId().value())
                .append(TYPE, item.type())
                .append(MESSAGE, item.message())
                .append(REF_SLUG, item.refSlug().orElse(null))
                .append(OCCURRED_AT, item.occurredAtEpochMillis());
    }

    public static ActivityItem fromDocument(Document document) {
        return new ActivityItem(
                TypedId.of(document.getString(ID)),
                TypedId.of(document.getString(USER_ID)),
                document.getString(TYPE),
                document.getString(MESSAGE),
                Optional.ofNullable(document.getString(REF_SLUG)),
                document.getLong(OCCURRED_AT));
    }
}
