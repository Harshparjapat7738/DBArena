package com.DBArena.services.gamification.repository.dailychallenge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.gamification.domain.dailychallenge.DailyChallengeCompletion;
import org.bson.Document;

public final class DailyChallengeCompletionDocumentMapper {

    static final String ID = "_id";
    static final String USER_ID = "userId";
    static final String DATE = "date";
    static final String COMPLETED_AT = "completedAt";
    static final String XP_AWARDED = "xpAwarded";

    private DailyChallengeCompletionDocumentMapper() {
    }

    public static Document toDocument(DailyChallengeCompletion completion) {
        return new Document()
                .append(ID, completion.id().value())
                .append(USER_ID, completion.userId().value())
                .append(DATE, completion.date())
                .append(COMPLETED_AT, completion.completedAtEpochMillis())
                .append(XP_AWARDED, completion.xpAwarded());
    }

    public static DailyChallengeCompletion fromDocument(Document document) {
        return new DailyChallengeCompletion(
                TypedId.of(document.getString(ID)),
                TypedId.of(document.getString(USER_ID)),
                document.getString(DATE),
                document.getLong(COMPLETED_AT),
                document.getInteger(XP_AWARDED, 0));
    }
}
