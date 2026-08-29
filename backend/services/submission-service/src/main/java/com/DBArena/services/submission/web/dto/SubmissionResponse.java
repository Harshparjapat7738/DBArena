package com.DBArena.services.submission.web.dto;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.submission.domain.Submission;
import com.DBArena.services.submission.domain.SubmissionStatus;

public record SubmissionResponse(
        String id,
        String problemSlug,
        EngineType engine,
        SubmissionStatus status,
        Long executionMs,
        Integer rowsReturned,
        Integer testsPassed,
        Integer testsTotal,
        Integer score,
        Double planCost,
        Long rowsExamined,
        String message,
        long submittedAtEpochMillis,
        Long completedAtEpochMillis) {

    public static SubmissionResponse from(Submission submission) {
        return new SubmissionResponse(
                submission.id().value(),
                submission.problemSlug(),
                submission.engine(),
                submission.status(),
                submission.executionMs().orElse(null),
                submission.rowsReturned().orElse(null),
                submission.testsPassed().orElse(null),
                submission.testsTotal().orElse(null),
                submission.score().orElse(null),
                submission.planCost().orElse(null),
                submission.rowsExamined().orElse(null),
                submission.message().orElse(null),
                submission.submittedAtEpochMillis(),
                submission.completedAtEpochMillis().orElse(null));
    }
}
