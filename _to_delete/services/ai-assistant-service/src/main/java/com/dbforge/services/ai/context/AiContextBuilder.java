package com.dbforge.services.ai.context;

import com.dbforge.common.core.value.CdmValue;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.cdm.CdmEntity;
import com.dbforge.engine.spi.cdm.CdmRow;
import com.dbforge.services.ai.client.CatalogProblemResponse;
import com.dbforge.services.ai.domain.HintCommand;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a catalog problem + an optional dataset + a learner's request into
 * a {@link HintContext} small enough for a compact prompt. This class is
 * the one place hard rule #5 ("the AI context builder has a hard-coded
 * 10-row-per-entity cap ... not a config value, not overridable, and not
 * adjustable by prompt") is actually implemented - every other class in
 * this service only ever sees data that has already passed through here.
 */
@Component
public class AiContextBuilder {

    /**
     * Hard rule #5, verbatim: not a {@code @ConfigurationProperties} field,
     * not a constructor parameter, not read from any request - a
     * {@code public static final} so it is visible at the call site and at
     * review time, and so nothing anywhere in this service can raise it
     * without editing this line.
     */
    public static final int MAX_SAMPLE_ROWS_PER_ENTITY = 10;

    private static final int MAX_STATEMENT_CHARS = 800;
    private static final int MAX_LEARNER_QUERY_CHARS = 1500;
    private static final int MAX_LEARNER_NOTES_CHARS = 800;

    public HintContext build(CatalogProblemResponse problem, Optional<CdmDataset> dataset, HintCommand command) {
        List<EntitySummary> entities = dataset.map(this::summarizeEntities).orElseGet(List::of);
        return new HintContext(
                problem.title(),
                truncate(problem.statementMarkdown(), MAX_STATEMENT_CHARS),
                problem.difficulty(),
                problem.tags(),
                problem.allowedEngines(),
                entities,
                truncate(command.learnerQuery(), MAX_LEARNER_QUERY_CHARS),
                truncate(command.errorOrResultText().orElse(""), MAX_LEARNER_NOTES_CHARS),
                command.level());
    }

    private List<EntitySummary> summarizeEntities(CdmDataset dataset) {
        return dataset.entities().stream().map(this::summarizeEntity).toList();
    }

    private EntitySummary summarizeEntity(CdmEntity entity) {
        List<ColumnSummary> columns = entity.columns().stream()
                .map(c -> new ColumnSummary(c.name(), c.type().name(), c.nullable(), c.primaryKey()))
                .toList();
        List<Map<String, String>> sampleRows = entity.seedRows().stream()
                .limit(MAX_SAMPLE_ROWS_PER_ENTITY)
                .map(this::renderRow)
                .toList();
        return new EntitySummary(entity.name(), columns, sampleRows);
    }

    private Map<String, String> renderRow(CdmRow row) {
        Map<String, String> rendered = new LinkedHashMap<>();
        row.values().forEach((column, value) -> rendered.put(column, renderValue(value)));
        return rendered;
    }

    private String renderValue(CdmValue value) {
        return switch (value) {
            case CdmValue.Null ignored -> "null";
            case CdmValue.Bool b -> Boolean.toString(b.value());
            case CdmValue.Int i -> Long.toString(i.value());
            case CdmValue.Decimal d -> d.toBigDecimal().toPlainString();
            case CdmValue.Text t -> t.value();
            case CdmValue.Timestamp ts -> Instant.ofEpochMilli(ts.epochMillis()).toString();
            case CdmValue.Json j -> j.canonicalJson();
        };
    }

    private String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars) + "…";
    }
}
