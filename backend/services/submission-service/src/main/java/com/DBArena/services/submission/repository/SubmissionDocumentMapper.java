package com.DBArena.services.submission.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.submission.domain.Submission;
import com.DBArena.services.submission.domain.SubmissionStatus;
import org.bson.Document;

import java.util.Optional;

public final class SubmissionDocumentMapper {

    static final String ID = "_id";
    static final String USER_ID = "userId";
    static final String PROBLEM_SLUG = "problemSlug";
    static final String ENGINE = "engine";
    static final String QUERY_TEXT = "queryText";
    static final String STATUS = "status";
    static final String EXECUTION_MS = "executionMs";
    static final String ROWS_RETURNED = "rowsReturned";
    static final String TESTS_PASSED = "testsPassed";
    static final String TESTS_TOTAL = "testsTotal";
    static final String SCORE = "score";
    static final String PLAN_COST = "planCost";
    static final String ROWS_EXAMINED = "rowsExamined";
    static final String MESSAGE = "message";
    static final String SUBMITTED_AT = "submittedAt";
    static final String COMPLETED_AT = "completedAt";

    private SubmissionDocumentMapper() {
    }

    public static Document toDocument(Submission submission) {
        return new Document()
                .append(ID, submission.id().value())
                .append(USER_ID, submission.userId().value())
                .append(PROBLEM_SLUG, submission.problemSlug())
                .append(ENGINE, submission.engine().name())
                .append(QUERY_TEXT, submission.queryText())
                .append(STATUS, submission.status().name())
                .append(EXECUTION_MS, submission.executionMs().orElse(null))
                .append(ROWS_RETURNED, submission.rowsReturned().orElse(null))
                .append(TESTS_PASSED, submission.testsPassed().orElse(null))
                .append(TESTS_TOTAL, submission.testsTotal().orElse(null))
                .append(SCORE, submission.score().orElse(null))
                .append(PLAN_COST, submission.planCost().orElse(null))
                .append(ROWS_EXAMINED, submission.rowsExamined().orElse(null))
                .append(MESSAGE, submission.message().orElse(null))
                .append(SUBMITTED_AT, submission.submittedAtEpochMillis())
                .append(COMPLETED_AT, submission.completedAtEpochMillis().orElse(null));
    }

    public static Submission fromDocument(Document document) {
        return new Submission(
                TypedId.of(document.getString(ID)),
                TypedId.<AuthenticatedUser>of(document.getString(USER_ID)),
                document.getString(PROBLEM_SLUG),
                EngineType.valueOf(document.getString(ENGINE)),
                document.getString(QUERY_TEXT),
                SubmissionStatus.valueOf(document.getString(STATUS)),
                Optional.ofNullable(document.getLong(EXECUTION_MS)),
                Optional.ofNullable(document.getInteger(ROWS_RETURNED)),
                Optional.ofNullable(document.getInteger(TESTS_PASSED)),
                Optional.ofNullable(document.getInteger(TESTS_TOTAL)),
                Optional.ofNullable(document.getInteger(SCORE)),
                Optional.ofNullable(document.getDouble(PLAN_COST)),
                Optional.ofNullable(document.getLong(ROWS_EXAMINED)),
                Optional.ofNullable(document.getString(MESSAGE)),
                document.getLong(SUBMITTED_AT),
                Optional.ofNullable(document.getLong(COMPLETED_AT)));
    }
}
