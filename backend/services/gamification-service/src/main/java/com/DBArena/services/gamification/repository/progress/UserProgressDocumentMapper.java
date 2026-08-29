package com.DBArena.services.gamification.repository.progress;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.progress.SkillMastery;
import com.DBArena.services.gamification.domain.progress.UserProgress;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public final class UserProgressDocumentMapper {

    static final String ID = "_id"; // the userId value itself - see UserProgress's Javadoc
    static final String XP = "xp";
    static final String LEVEL = "level";
    static final String XP_INTO_LEVEL = "xpIntoLevel";
    static final String XP_FOR_NEXT_LEVEL = "xpForNextLevel";
    static final String STREAK_CURRENT = "streakCurrent";
    static final String STREAK_LONGEST = "streakLongest";
    static final String STREAK_LAST_ACTIVE_DATE = "streakLastActiveDate";
    static final String FREEZES_AVAILABLE = "freezesAvailable";
    static final String MASTERIES = "masteries";
    static final String UPDATED_AT = "updatedAt";

    private static final String MASTERY_TOPIC = "topic";
    private static final String MASTERY_PCT = "masteryPct";
    private static final String MASTERY_SOLVED = "problemsSolved";
    private static final String MASTERY_TOTAL = "problemsTotal";

    private UserProgressDocumentMapper() {
    }

    public static Document toDocument(UserProgress progress) {
        return new Document()
                .append(ID, progress.userId().value())
                .append(XP, progress.xp())
                .append(LEVEL, progress.level())
                .append(XP_INTO_LEVEL, progress.xpIntoLevel())
                .append(XP_FOR_NEXT_LEVEL, progress.xpForNextLevel())
                .append(STREAK_CURRENT, progress.streakCurrent())
                .append(STREAK_LONGEST, progress.streakLongest())
                .append(STREAK_LAST_ACTIVE_DATE, progress.streakLastActiveDate().orElse(null))
                .append(FREEZES_AVAILABLE, progress.freezesAvailable())
                .append(MASTERIES, progress.masteries().stream().map(UserProgressDocumentMapper::masteryToDocument).toList())
                .append(UPDATED_AT, progress.updatedAtEpochMillis());
    }

    public static UserProgress fromDocument(Document document) {
        List<SkillMastery> masteries = document.getList(MASTERIES, Document.class, List.of()).stream()
                .map(UserProgressDocumentMapper::masteryFromDocument)
                .toList();

        return new UserProgress(
                TypedId.<AuthenticatedUser>of(document.getString(ID)),
                document.getLong(XP),
                document.getInteger(LEVEL, 1),
                document.getLong(XP_INTO_LEVEL),
                document.getLong(XP_FOR_NEXT_LEVEL),
                document.getInteger(STREAK_CURRENT, 0),
                document.getInteger(STREAK_LONGEST, 0),
                Optional.ofNullable(document.getString(STREAK_LAST_ACTIVE_DATE)),
                document.getInteger(FREEZES_AVAILABLE, 0),
                masteries,
                document.getLong(UPDATED_AT));
    }

    private static Document masteryToDocument(SkillMastery mastery) {
        return new Document()
                .append(MASTERY_TOPIC, mastery.topic())
                .append(MASTERY_PCT, mastery.masteryPct())
                .append(MASTERY_SOLVED, mastery.problemsSolved())
                .append(MASTERY_TOTAL, mastery.problemsTotal());
    }

    private static SkillMastery masteryFromDocument(Document document) {
        return new SkillMastery(
                document.getString(MASTERY_TOPIC),
                document.getInteger(MASTERY_PCT, 0),
                document.getInteger(MASTERY_SOLVED, 0),
                document.getInteger(MASTERY_TOTAL, 0));
    }
}
