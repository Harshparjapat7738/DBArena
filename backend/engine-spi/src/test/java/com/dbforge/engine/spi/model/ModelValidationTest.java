package com.dbforge.engine.spi.model;

import com.dbforge.common.core.value.CdmValue;
import com.dbforge.engine.spi.EngineType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelValidationTest {

    @Test
    void sessionHandleRejectsBlankFields() {
        assertThatThrownBy(() -> new SessionHandle("", EngineType.POSTGRES, "ref"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionHandle("id", EngineType.POSTGRES, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // DatasetDescriptor was replaced by com.dbforge.engine.spi.cdm.CdmDataset
    // in M02 - see that package's CdmModelTest for its coverage.

    @Test
    void statementRequestRejectsNonPositiveTimeout() {
        assertThatThrownBy(() -> new StatementRequest("SELECT 1", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StatementRequest(" ", Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void executionResultFailureFactoryHasNoRowsAndAnError() {
        ExecutionResult result = ExecutionResult.failure(new ExecutionError("syntax_error", "bad token"), 12);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.rows()).isEmpty();
        assertThat(result.columns()).isEmpty();
        assertThat(result.error()).isPresent();
    }

    @Test
    void executionResultSuccessCase() {
        ColumnMeta col = new ColumnMeta("id", "Int", false);
        ResultRow row = new ResultRow(List.of(new CdmValue.Int(1)));
        ExecutionResult result = new ExecutionResult(List.of(col), List.of(row), 5, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.rows()).hasSize(1);
    }

    @Test
    void materializationResultCopiesMap() {
        MaterializationResult result = new MaterializationResult(
                new SessionHandle("s1", EngineType.MONGODB, "db1"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Map.of("orders", 10L));

        assertThat(result.rowCountsByEntity()).containsEntry("orders", 10L);
    }

    @Test
    void explainPlanRejectsBlankText() {
        assertThatThrownBy(() -> new ExplainPlan("", java.util.Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entitySchemaAndSchemaSnapshotHoldColumns() {
        EntitySchema entity = new EntitySchema("orders", List.of(new ColumnMeta("id", "Int", false)));
        SchemaSnapshot snapshot = new SchemaSnapshot(List.of(entity));

        assertThat(snapshot.entities()).containsExactly(entity);
    }

    @Test
    void executionErrorRejectsBlankFields() {
        assertThatThrownBy(() -> new ExecutionError("", "message"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExecutionError("code", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void columnMetaRejectsBlankFields() {
        assertThatThrownBy(() -> new ColumnMeta("", "Int", false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ColumnMeta("id", "", false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
