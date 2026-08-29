package com.DBArena.services.execution.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.ColumnMeta;
import com.DBArena.services.execution.domain.Execution;
import com.DBArena.services.execution.domain.ExecutionMetrics;
import com.DBArena.services.execution.domain.ExecutionResultSummary;
import com.DBArena.services.execution.domain.ExecutionStatus;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionDocumentMapperTest {

    @Test
    void roundTripsAFreshlyRequestedExecutionWithNoResultYet() {
        Execution execution = Execution.requested(
                TypedId.of("01J000EXEC"), TypedId.of("01J000USER"), EngineType.POSTGRES,
                "two-sum", Optional.of("two-sum"), "SELECT 1", 1_700_000_000_000L);

        Document document = ExecutionDocumentMapper.toDocument(execution);
        Execution roundTripped = ExecutionDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(execution);
        assertThat(roundTripped.status()).isEqualTo(ExecutionStatus.REQUESTED);
    }

    @Test
    void roundTripsACompletedExecutionWithResultAndMetrics() {
        Execution requested = Execution.requested(
                TypedId.of("01J000EXEC2"), TypedId.of("01J000USER"), EngineType.POSTGRES,
                "two-sum", Optional.empty(), "SELECT id FROM orders", 1_700_000_000_000L);

        ExecutionResultSummary result = new ExecutionResultSummary(
                List.of(new ColumnMeta("id", "Int", false)), List.of(List.of("1"), List.of("2")), false);
        ExecutionMetrics metrics = new ExecutionMetrics(42L, Optional.of(5L), 2, 128L);
        Execution completed = requested.withStarted(1_700_000_000_100L)
                .withStatus(ExecutionStatus.EXECUTING)
                .withCompleted(result, metrics, 1_700_000_000_200L);

        Document document = ExecutionDocumentMapper.toDocument(completed);
        Execution roundTripped = ExecutionDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(completed);
        assertThat(roundTripped.result()).isPresent();
        assertThat(roundTripped.metrics().orElseThrow().planningTimeMillis()).contains(5L);
    }

    @Test
    void roundTripsARejectedExecution() {
        Execution rejected = Execution.requested(
                        TypedId.of("01J000EXEC3"), TypedId.of("01J000USER"), EngineType.POSTGRES,
                        "two-sum", Optional.empty(), "DROP TABLE orders", 1L)
                .withRejected("only SELECT statements are allowed", 2L);

        Document document = ExecutionDocumentMapper.toDocument(rejected);
        Execution roundTripped = ExecutionDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(rejected);
        assertThat(roundTripped.rejectionReason()).contains("only SELECT statements are allowed");
    }
}
