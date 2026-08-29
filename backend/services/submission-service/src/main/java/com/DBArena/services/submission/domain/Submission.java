package com.DBArena.services.submission.domain;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.engine.spi.EngineType;

import java.util.Optional;

/**
 * A single graded (or grading) attempt. This is the highest-write-volume
 * collection in the platform once execution-service/B11 exist - every
 * "Run" or "Submit" click in the workbench inserts one - which drives every
 * design choice here:
 *
 * <ul>
 *   <li>Id is a ULID (via {@link com.DBArena.common.core.id.UlidIdGenerator}),
 *       not a Mongo default ObjectId, so ids stay roughly time-ordered and
 *       insert-cheap even at high write rates, and so a future shard key can
 *       use it without a random-insert hotspot.</li>
 *   <li>{@code queryText} is bounded ({@code @Size} on the request DTO) -
 *       this collection must never be asked to hold a raw result set or an
 *       unbounded blob; only summary statistics of a run are stored here.</li>
 *   <li>Every result field is {@code Optional} because a submission is
 *       inserted at {@code PENDING} before grading has run - there is no
 *       execution engine yet to populate them (B02 explicitly excludes
 *       execution; {@link #pending} is the only factory this milestone
 *       needs).</li>
 * </ul>
 */
public record Submission(
        TypedId<Submission> id,
        TypedId<AuthenticatedUser> userId,
        String problemSlug,
        EngineType engine,
        String queryText,
        SubmissionStatus status,
        Optional<Long> executionMs,
        Optional<Integer> rowsReturned,
        Optional<Integer> testsPassed,
        Optional<Integer> testsTotal,
        Optional<Integer> score,
        Optional<Double> planCost,
        Optional<Long> rowsExamined,
        Optional<String> message,
        long submittedAtEpochMillis,
        Optional<Long> completedAtEpochMillis) {

    public Submission {
        if (problemSlug == null || problemSlug.isBlank()) {
            throw new IllegalArgumentException("problemSlug must not be blank");
        }
        if (queryText == null || queryText.isBlank()) {
            throw new IllegalArgumentException("queryText must not be blank");
        }
        executionMs = executionMs == null ? Optional.empty() : executionMs;
        rowsReturned = rowsReturned == null ? Optional.empty() : rowsReturned;
        testsPassed = testsPassed == null ? Optional.empty() : testsPassed;
        testsTotal = testsTotal == null ? Optional.empty() : testsTotal;
        score = score == null ? Optional.empty() : score;
        planCost = planCost == null ? Optional.empty() : planCost;
        rowsExamined = rowsExamined == null ? Optional.empty() : rowsExamined;
        message = message == null ? Optional.empty() : message;
        completedAtEpochMillis = completedAtEpochMillis == null ? Optional.empty() : completedAtEpochMillis;
    }

    public static Submission pending(
            TypedId<Submission> id,
            TypedId<AuthenticatedUser> userId,
            String problemSlug,
            EngineType engine,
            String queryText,
            long submittedAtEpochMillis) {
        return new Submission(id, userId, problemSlug, engine, queryText, SubmissionStatus.PENDING,
                Optional.<Long>empty(), Optional.<Integer>empty(), Optional.<Integer>empty(), Optional.<Integer>empty(),
                Optional.<Integer>empty(), Optional.<Double>empty(), Optional.<Long>empty(), Optional.<String>empty(),
                submittedAtEpochMillis, Optional.<Long>empty());
    }
}
