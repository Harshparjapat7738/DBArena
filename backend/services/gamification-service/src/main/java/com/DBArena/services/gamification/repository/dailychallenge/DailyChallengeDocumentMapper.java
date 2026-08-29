package com.DBArena.services.gamification.repository.dailychallenge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.gamification.domain.dailychallenge.DailyChallenge;
import org.bson.Document;

public final class DailyChallengeDocumentMapper {

    static final String ID = "_id";
    static final String DATE = "date";
    static final String PROBLEM_SLUG = "problemSlug";
    static final String ENGINE = "engine";
    static final String TOPIC = "topic";
    static final String ESTIMATED_MINUTES = "estimatedMinutes";
    static final String XP_REWARD = "xpReward";

    private DailyChallengeDocumentMapper() {
    }

    public static Document toDocument(DailyChallenge challenge) {
        return new Document()
                .append(ID, challenge.id().value())
                .append(DATE, challenge.date())
                .append(PROBLEM_SLUG, challenge.problemSlug())
                .append(ENGINE, challenge.engine().name())
                .append(TOPIC, challenge.topic())
                .append(ESTIMATED_MINUTES, challenge.estimatedMinutes())
                .append(XP_REWARD, challenge.xpReward());
    }

    public static DailyChallenge fromDocument(Document document) {
        return new DailyChallenge(
                TypedId.of(document.getString(ID)),
                document.getString(DATE),
                document.getString(PROBLEM_SLUG),
                EngineType.valueOf(document.getString(ENGINE)),
                document.getString(TOPIC),
                document.getInteger(ESTIMATED_MINUTES, 0),
                document.getInteger(XP_REWARD, 0));
    }
}
