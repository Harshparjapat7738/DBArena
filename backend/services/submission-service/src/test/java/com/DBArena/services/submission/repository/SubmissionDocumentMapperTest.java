package com.DBArena.services.submission.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.submission.domain.Submission;
import com.DBArena.services.submission.domain.SubmissionStatus;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionDocumentMapperTest {

    @Test
    void roundTripsAPendingSubmissionWithNoResultFieldsYet() {
        Submission submission = Submission.pending(
                TypedId.of("01J000SUBMISSION"), TypedId.of("01J000USER"), "two-sum",
                EngineType.POSTGRES, "SELECT 1;", 1_700_000_000_000L);

        Document document = SubmissionDocumentMapper.toDocument(submission);
        Submission roundTripped = SubmissionDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(submission);
        assertThat(roundTripped.status()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(roundTripped.executionMs()).isEmpty();
    }

    @Test
    void roundTripsAGradedSubmissionWithEveryResultFieldPresent() {
        Submission submission = new Submission(
                TypedId.of("01J000SUBMISSION2"),
                TypedId.<AuthenticatedUser>of("01J000USER"),
                "two-sum",
                EngineType.MONGODB,
                "db.orders.find({})",
                SubmissionStatus.ACCEPTED,
                Optional.of(42L),
                Optional.of(10),
                Optional.of(5),
                Optional.of(5),
                Optional.of(100),
                Optional.of(12.5),
                Optional.of(200L),
                Optional.of("all tests passed"),
                1_700_000_000_000L,
                Optional.of(1_700_000_000_500L));

        Document document = SubmissionDocumentMapper.toDocument(submission);
        Submission roundTripped = SubmissionDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(submission);
    }
}
