package com.dbforge.services.ai.context;

import com.dbforge.common.core.value.CdmValue;
import com.dbforge.engine.spi.cdm.CdmColumn;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.cdm.CdmEntity;
import com.dbforge.engine.spi.cdm.CdmRow;
import com.dbforge.engine.spi.cdm.CdmType;
import com.dbforge.services.ai.client.CatalogProblemResponse;
import com.dbforge.services.ai.domain.HintCommand;
import com.dbforge.services.ai.domain.HintLevel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiContextBuilderTest {

    private final AiContextBuilder builder = new AiContextBuilder();

    private static CatalogProblemResponse problem(String statement) {
        return new CatalogProblemResponse("two-sum", "Two Sum", statement, "EASY",
                Set.of("arrays"), Set.of("POSTGRES"), "two-sum", true);
    }

    private static HintCommand command(String query, HintLevel level) {
        return new HintCommand("two-sum", query, Optional.empty(), level);
    }

    private static CdmEntity entityWithRows(int rowCount) {
        List<CdmColumn> columns = List.of(new CdmColumn("id", CdmType.INTEGER, false, true));
        List<CdmRow> rows = java.util.stream.IntStream.range(0, rowCount)
                .mapToObj(i -> {
                    Map<String, CdmValue> values = new LinkedHashMap<>();
                    values.put("id", new CdmValue.Int(i));
                    return new CdmRow(values);
                })
                .toList();
        return new CdmEntity("numbers", columns, List.of(), rows);
    }

    @Test
    void neverEmitsMoreThanTenSampleRowsPerEntityEvenWhenTheDatasetHasMore() {
        CdmDataset dataset = new CdmDataset("d1", "D1", 1, List.of(entityWithRows(37)));
        HintContext context = builder.build(problem("stmt"), Optional.of(dataset),
                command("select 1", HintLevel.CONCEPT));

        assertThat(context.entities()).hasSize(1);
        assertThat(context.entities().get(0).sampleRows()).hasSize(AiContextBuilder.MAX_SAMPLE_ROWS_PER_ENTITY);
    }

    @Test
    void theRowCapConstantIsExactlyTen() {
        // Hard rule #5, pinned literally - a change here should force a
        // human to re-read the hard rule before "fixing" this test.
        assertThat(AiContextBuilder.MAX_SAMPLE_ROWS_PER_ENTITY).isEqualTo(10);
    }

    @Test
    void aDatasetWithFewerRowsThanTheCapKeepsThemAll() {
        CdmDataset dataset = new CdmDataset("d1", "D1", 1, List.of(entityWithRows(3)));
        HintContext context = builder.build(problem("stmt"), Optional.of(dataset),
                command("select 1", HintLevel.CONCEPT));

        assertThat(context.entities().get(0).sampleRows()).hasSize(3);
    }

    @Test
    void aMissingDatasetProducesAnEmptyEntityListNotAnError() {
        HintContext context = builder.build(problem("stmt"), Optional.empty(), command("select 1", HintLevel.CONCEPT));

        assertThat(context.entities()).isEmpty();
    }

    @Test
    void aLongStatementIsTruncatedWithAMarker() {
        String longStatement = "x".repeat(5000);
        HintContext context = builder.build(problem(longStatement), Optional.empty(),
                command("select 1", HintLevel.CONCEPT));

        assertThat(context.statementExcerpt()).hasSizeLessThan(5000);
        assertThat(context.statementExcerpt()).endsWith("…");
    }

    @Test
    void aShortStatementIsNotTouched() {
        HintContext context = builder.build(problem("short"), Optional.empty(), command("select 1", HintLevel.CONCEPT));

        assertThat(context.statementExcerpt()).isEqualTo("short");
    }

    @Test
    void aLongLearnerQueryIsTruncated() {
        String longQuery = "select ".repeat(1000);
        HintContext context = builder.build(problem("stmt"), Optional.empty(), command(longQuery, HintLevel.NEAR_MISS));

        assertThat(context.learnerQueryExcerpt().length()).isLessThan(longQuery.length());
    }

    @Test
    void everyCdmValueVariantRendersAsPlainTextNotAJavaToString() {
        Map<String, CdmValue> values = new LinkedHashMap<>();
        values.put("a", CdmValue.Null.INSTANCE);
        values.put("b", new CdmValue.Bool(true));
        values.put("c", new CdmValue.Int(42));
        values.put("d", CdmValue.Decimal.of(new java.math.BigDecimal("1.50")));
        values.put("e", new CdmValue.Text("hi"));
        values.put("f", new CdmValue.Timestamp(0L));
        values.put("g", new CdmValue.Json("{\"k\":1}"));
        CdmEntity entity = new CdmEntity("things",
                List.of(new CdmColumn("a", CdmType.BOOLEAN, true, false)),
                List.of(), List.of(new CdmRow(values)));
        CdmDataset dataset = new CdmDataset("d1", "D1", 1, List.of(entity));

        HintContext context = builder.build(problem("stmt"), Optional.of(dataset), command("q", HintLevel.CONCEPT));
        Map<String, String> row = context.entities().get(0).sampleRows().get(0);

        assertThat(row.get("a")).isEqualTo("null");
        assertThat(row.get("b")).isEqualTo("true");
        assertThat(row.get("c")).isEqualTo("42");
        assertThat(row.get("d")).isEqualTo("1.50");
        assertThat(row.get("e")).isEqualTo("hi");
        assertThat(row.get("f")).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(row.get("g")).isEqualTo("{\"k\":1}");
    }
}
