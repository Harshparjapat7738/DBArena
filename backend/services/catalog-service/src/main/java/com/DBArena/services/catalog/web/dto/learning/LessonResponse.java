package com.DBArena.services.catalog.web.dto.learning;

import com.DBArena.services.catalog.domain.learning.Lesson;

public record LessonResponse(
        String slug,
        String title,
        String summary,
        int durationMinutes,
        int order,
        String practiceProblemSlug) {

    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(lesson.slug(), lesson.title(), lesson.summary(),
                lesson.durationMinutes(), lesson.order(), lesson.practiceProblemSlug().orElse(null));
    }
}
