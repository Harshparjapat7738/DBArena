package com.DBArena.services.execution.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.ColumnMeta;
import com.DBArena.services.execution.domain.Execution;
import com.DBArena.services.execution.domain.ExecutionMetrics;
import com.DBArena.services.execution.domain.ExecutionResultSummary;
import com.DBArena.services.execution.domain.ExecutionStatus;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public final class ExecutionDocumentMapper {

    static final String ID = "_id";
    static final String USER_ID = "userId";
    static final String ENGINE = "engine";
    static final String DATASET_SLUG = "datasetSlug";
    static final String PROBLEM_SLUG = "problemSlug";
    static final String STATEMENT_TEXT = "statementText";
    static final String STATUS = "status";
    static final String REQUESTED_AT = "requestedAt";
    static final String STARTED_AT = "startedAt";
    static final String COMPLETED_AT = "completedAt";
    static final String RESULT = "result";
    static final String METRICS = "metrics";
    static final String REJECTION_REASON = "rejectionReason";
    static final String ERROR_MESSAGE = "errorMessage";

    private static final String RESULT_COLUMNS = "columns";
    private static final String RESULT_ROWS = "rows";
    private static final String RESULT_TRUNCATED = "truncated";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_TYPE = "cdmTypeName";
    private static final String COLUMN_NULLABLE = "nullable";

    private static final String METRICS_EXECUTION_MS = "executionTimeMillis";
    private static final String METRICS_PLANNING_MS = "planningTimeMillis";
    private static final String METRICS_ROWS_RETURNED = "rowsReturned";
    private static final String METRICS_RESULT_SIZE_BYTES = "resultSizeBytes";

    private ExecutionDocumentMapper() {
    }

    public static Document toDocument(Execution execution) {
        Document document = new Document()
                .append(ID, execution.id().value())
                .append(USER_ID, execution.userId().value())
                .append(ENGINE, execution.engine().name())
                .append(DATASET_SLUG, execution.datasetSlug())
                .append(PROBLEM_SLUG, execution.problemSlug().orElse(null))
                .append(STATEMENT_TEXT, execution.statementText())
                .append(STATUS, execution.status().name())
                .append(REQUESTED_AT, execution.requestedAtEpochMillis())
                .append(STARTED_AT, execution.startedAtEpochMillis().orElse(null))
                .append(COMPLETED_AT, execution.completedAtEpochMillis().orElse(null))
                .append(REJECTION_REASON, execution.rejectionReason().orElse(null))
                .append(ERROR_MESSAGE, execution.errorMessage().orElse(null));
        execution.result().ifPresent(result -> document.append(RESULT, resultToDocument(result)));
        execution.metrics().ifPresent(metrics -> document.append(METRICS, metricsToDocument(metrics)));
        return document;
    }

    public static Execution fromDocument(Document document) {
        Optional<ExecutionResultSummary> result = Optional.ofNullable(document.get(RESULT, Document.class))
                .map(ExecutionDocumentMapper::resultFromDocument);
        Optional<ExecutionMetrics> metrics = Optional.ofNullable(document.get(METRICS, Document.class))
                .map(ExecutionDocumentMapper::metricsFromDocument);

        return new Execution(
                TypedId.of(document.getString(ID)),
                TypedId.<AuthenticatedUser>of(document.getString(USER_ID)),
                EngineType.valueOf(document.getString(ENGINE)),
                document.getString(DATASET_SLUG),
                Optional.ofNullable(document.getString(PROBLEM_SLUG)),
                document.getString(STATEMENT_TEXT),
                ExecutionStatus.valueOf(document.getString(STATUS)),
                document.getLong(REQUESTED_AT),
                Optional.ofNullable(document.getLong(STARTED_AT)),
                Optional.ofNullable(document.getLong(COMPLETED_AT)),
                result,
                metrics,
                Optional.ofNullable(document.getString(REJECTION_REASON)),
                Optional.ofNullable(document.getString(ERROR_MESSAGE)));
    }

    private static Document resultToDocument(ExecutionResultSummary result) {
        return new Document()
                .append(RESULT_COLUMNS, result.columns().stream().map(ExecutionDocumentMapper::columnToDocument).toList())
                .append(RESULT_ROWS, result.rows())
                .append(RESULT_TRUNCATED, result.truncated());
    }

    @SuppressWarnings("unchecked")
    private static ExecutionResultSummary resultFromDocument(Document document) {
        List<ColumnMeta> columns = document.getList(RESULT_COLUMNS, Document.class, List.of()).stream()
                .map(ExecutionDocumentMapper::columnFromDocument)
                .toList();
        List<List<String>> rows = (List<List<String>>) (List<?>) document.getList(RESULT_ROWS, List.class, List.of());
        boolean truncated = Boolean.TRUE.equals(document.getBoolean(RESULT_TRUNCATED));
        return new ExecutionResultSummary(columns, rows, truncated);
    }

    private static Document columnToDocument(ColumnMeta column) {
        return new Document()
                .append(COLUMN_NAME, column.name())
                .append(COLUMN_TYPE, column.cdmTypeName())
                .append(COLUMN_NULLABLE, column.nullable());
    }

    private static ColumnMeta columnFromDocument(Document document) {
        return new ColumnMeta(
                document.getString(COLUMN_NAME),
                document.getString(COLUMN_TYPE),
                Boolean.TRUE.equals(document.getBoolean(COLUMN_NULLABLE)));
    }

    private static Document metricsToDocument(ExecutionMetrics metrics) {
        return new Document()
                .append(METRICS_EXECUTION_MS, metrics.executionTimeMillis())
                .append(METRICS_PLANNING_MS, metrics.planningTimeMillis().orElse(null))
                .append(METRICS_ROWS_RETURNED, metrics.rowsReturned())
                .append(METRICS_RESULT_SIZE_BYTES, metrics.resultSizeBytes());
    }

    private static ExecutionMetrics metricsFromDocument(Document document) {
        return new ExecutionMetrics(
                document.getLong(METRICS_EXECUTION_MS),
                Optional.ofNullable(document.getLong(METRICS_PLANNING_MS)),
                document.getInteger(METRICS_ROWS_RETURNED, 0),
                document.getLong(METRICS_RESULT_SIZE_BYTES));
    }
}
