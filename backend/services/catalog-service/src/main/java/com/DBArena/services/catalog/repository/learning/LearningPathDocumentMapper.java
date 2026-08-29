package com.DBArena.services.catalog.repository.learning;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.learning.LearningLevel;
import com.DBArena.services.catalog.domain.learning.LearningPath;
import com.DBArena.services.catalog.domain.learning.Lesson;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public final class LearningPathDocumentMapper {

    static final String ID = "_id";
    static final String SLUG = "slug";
    static final String TITLE = "title";
    static final String DESCRIPTION = "description";
    static final String LEVEL = "level";
    static final String ENGINE = "engine";
    static final String LESSONS = "lessons";
    static final String ESTIMATED_HOURS = "estimatedHours";
    static final String VERSION = "version";
    static final String CREATED_AT = "createdAt";
    static final String UPDATED_AT = "updatedAt";

    private static final String LESSON_SLUG = "slug";
    private static final String LESSON_TITLE = "title";
    private static final String LESSON_SUMMARY = "summary";
    private static final String LESSON_DURATION_MINUTES = "durationMinutes";
    private static final String LESSON_ORDER = "order";
    private static final String LESSON_PRACTICE_PROBLEM_SLUG = "practiceProblemSlug";

    private LearningPathDocumentMapper() {
    }

    public static Document toDocument(LearningPath path) {
        return new Document()
                .append(ID, path.id().value())
                .append(SLUG, path.slug())
                .append(TITLE, path.title())
                .append(DESCRIPTION, path.description())
                .append(LEVEL, path.level().name())
                .append(ENGINE, path.engine().name())
                .append(LESSONS, path.lessons().stream().map(LearningPathDocumentMapper::lessonToDocument).toList())
                .append(ESTIMATED_HOURS, path.estimatedHours())
                .append(VERSION, path.version())
                .append(CREATED_AT, path.createdAtEpochMillis())
                .append(UPDATED_AT, path.updatedAtEpochMillis());
    }

    public static LearningPath fromDocument(Document document) {
        List<Lesson> lessons = document.getList(LESSONS, Document.class, List.of()).stream()
                .map(LearningPathDocumentMapper::lessonFromDocument)
                .toList();

        return new LearningPath(
                TypedId.of(document.getString(ID)),
                document.getString(SLUG),
                document.getString(TITLE),
                document.getString(DESCRIPTION),
                LearningLevel.valueOf(document.getString(LEVEL)),
                EngineType.valueOf(document.getString(ENGINE)),
                lessons,
                document.getInteger(ESTIMATED_HOURS, 0),
                document.getInteger(VERSION, 1),
                document.getLong(CREATED_AT),
                document.getLong(UPDATED_AT));
    }

    private static Document lessonToDocument(Lesson lesson) {
        return new Document()
                .append(LESSON_SLUG, lesson.slug())
                .append(LESSON_TITLE, lesson.title())
                .append(LESSON_SUMMARY, lesson.summary())
                .append(LESSON_DURATION_MINUTES, lesson.durationMinutes())
                .append(LESSON_ORDER, lesson.order())
                .append(LESSON_PRACTICE_PROBLEM_SLUG, lesson.practiceProblemSlug().orElse(null));
    }

    private static Lesson lessonFromDocument(Document document) {
        return new Lesson(
                document.getString(LESSON_SLUG),
                document.getString(LESSON_TITLE),
                document.getString(LESSON_SUMMARY),
                document.getInteger(LESSON_DURATION_MINUTES, 0),
                document.getInteger(LESSON_ORDER, 0),
                Optional.ofNullable(document.getString(LESSON_PRACTICE_PROBLEM_SLUG)));
    }
}
