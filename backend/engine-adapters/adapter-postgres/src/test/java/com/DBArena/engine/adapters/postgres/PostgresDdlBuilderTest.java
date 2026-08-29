package com.dbforge.engine.adapters.postgres;

import com.dbforge.engine.spi.cdm.CdmColumn;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.cdm.CdmEntity;
import com.dbforge.engine.spi.cdm.CdmForeignKey;
import com.dbforge.engine.spi.cdm.CdmType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link PostgresDdlBuilder} - no database involved.
 * Fixtures are hand-built {@link CdmDataset}/{@link CdmEntity} values, not
 * loaded from a dataset.yaml, since the point here is to pin down the exact
 * SQL text shape the builder produces (quoting, COLLATE "C", NOT NULL,
 * PRIMARY KEY clause placement, separate FK ALTER statements) rather than
 * to exercise a real dataset - that's the integration test's job.
 */
class PostgresDdlBuilderTest {

    @Test
    void createTableStatementQuotesIdentifiersAndOrdersColumnsAsDeclared() {
        CdmEntity numbers = new CdmEntity(
                "numbers",
                List.of(
                        new CdmColumn("id", CdmType.INTEGER, false, true),
                        new CdmColumn("label", CdmType.TEXT, false, false)),
                List.of(),
                List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(numbers));

        List<String> statements = PostgresDdlBuilder.createTableStatements(dataset);

        assertThat(statements).hasSize(1);
        String sql = statements.get(0);
        assertThat(sql).startsWith("CREATE TABLE \"numbers\" (\n");
        assertThat(sql).contains("\"id\" bigint NOT NULL");
        assertThat(sql).contains("\"label\" text COLLATE \"C\" NOT NULL");
        assertThat(sql).contains("PRIMARY KEY (\"id\")");
        // Column order preserved: id's definition appears before label's.
        assertThat(sql.indexOf("\"id\" bigint")).isLessThan(sql.indexOf("\"label\" text"));
        // PRIMARY KEY clause is the final part, after every column definition.
        assertThat(sql.indexOf("PRIMARY KEY")).isGreaterThan(sql.indexOf("\"label\" text"));
    }

    @Test
    void nullableNonTextColumnGetsNoCollateAndNoNotNull() {
        CdmEntity entity = new CdmEntity(
                "queries",
                List.of(
                        new CdmColumn("id", CdmType.INTEGER, false, true),
                        new CdmColumn("score", CdmType.DECIMAL, true, false)),
                List.of(),
                List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        String sql = PostgresDdlBuilder.createTableStatements(dataset).get(0);

        assertThat(sql).contains("\"score\" numeric");
        assertThat(sql).doesNotContain("\"score\" numeric COLLATE");
        assertThat(sql).doesNotContain("\"score\" numeric NOT NULL");
    }

    @Test
    void everyCdmTypeMapsToItsPostgresNativeTypeInColumnDefinitions() {
        CdmEntity entity = new CdmEntity(
                "all_types",
                List.of(
                        new CdmColumn("a", CdmType.BOOLEAN, true, false),
                        new CdmColumn("b", CdmType.INTEGER, true, false),
                        new CdmColumn("c", CdmType.DECIMAL, true, false),
                        new CdmColumn("d", CdmType.TEXT, true, false),
                        new CdmColumn("e", CdmType.TIMESTAMP, true, false),
                        new CdmColumn("f", CdmType.JSON, true, false)),
                List.of(),
                List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        String sql = PostgresDdlBuilder.createTableStatements(dataset).get(0);

        assertThat(sql).contains("\"a\" boolean");
        assertThat(sql).contains("\"b\" bigint");
        assertThat(sql).contains("\"c\" numeric");
        assertThat(sql).contains("\"d\" text COLLATE \"C\"");
        assertThat(sql).contains("\"e\" timestamptz");
        assertThat(sql).contains("\"f\" jsonb");
    }

    @Test
    void entityWithNoPrimaryKeyColumnsOmitsThePrimaryKeyClauseEntirely() {
        CdmEntity entity = new CdmEntity(
                "no_pk",
                List.of(new CdmColumn("note", CdmType.TEXT, true, false)),
                List.of(),
                List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        String sql = PostgresDdlBuilder.createTableStatements(dataset).get(0);

        assertThat(sql).doesNotContain("PRIMARY KEY");
    }

    @Test
    void multiColumnPrimaryKeyListsColumnsInDeclarationOrder() {
        CdmEntity entity = new CdmEntity(
                "composite",
                List.of(
                        new CdmColumn("tenant_id", CdmType.INTEGER, false, true),
                        new CdmColumn("item_id", CdmType.INTEGER, false, true),
                        new CdmColumn("payload", CdmType.TEXT, true, false)),
                List.of(),
                List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        String sql = PostgresDdlBuilder.createTableStatements(dataset).get(0);

        assertThat(sql).contains("PRIMARY KEY (\"tenant_id\", \"item_id\")");
    }

    @Test
    void createTableStatementsPreservesEntityDeclarationOrder() {
        CdmEntity first = new CdmEntity("first", List.of(new CdmColumn("id", CdmType.INTEGER, false, true)), List.of(), List.of());
        CdmEntity second = new CdmEntity("second", List.of(new CdmColumn("id", CdmType.INTEGER, false, true)), List.of(), List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(first, second));

        List<String> statements = PostgresDdlBuilder.createTableStatements(dataset);

        assertThat(statements).hasSize(2);
        assertThat(statements.get(0)).contains("\"first\"");
        assertThat(statements.get(1)).contains("\"second\"");
    }

    @Test
    void addForeignKeyStatementsProducesOneAlterTablePerForeignKeyWithAQuotedGeneratedConstraintName() {
        CdmEntity queries = new CdmEntity(
                "queries",
                List.of(
                        new CdmColumn("id", CdmType.INTEGER, false, true),
                        new CdmColumn("number_id", CdmType.INTEGER, true, false)),
                List.of(new CdmForeignKey("number_id", "numbers", "id")),
                List.of());

        List<String> statements = PostgresDdlBuilder.addForeignKeyStatements(queries);

        assertThat(statements).hasSize(1);
        assertThat(statements.get(0)).isEqualTo(
                "ALTER TABLE \"queries\" ADD CONSTRAINT \"fk_queries_number_id\""
                        + " FOREIGN KEY (\"number_id\") REFERENCES \"numbers\" (\"id\")");
    }

    @Test
    void addForeignKeyStatementsReturnsEmptyListWhenEntityHasNoForeignKeys() {
        CdmEntity entity = new CdmEntity("standalone", List.of(new CdmColumn("id", CdmType.INTEGER, false, true)), List.of(), List.of());

        assertThat(PostgresDdlBuilder.addForeignKeyStatements(entity)).isEmpty();
    }

    @Test
    void addForeignKeyStatementsHandlesMultipleForeignKeysOnTheSameEntity() {
        CdmEntity entity = new CdmEntity(
                "junction",
                List.of(
                        new CdmColumn("id", CdmType.INTEGER, false, true),
                        new CdmColumn("left_id", CdmType.INTEGER, false, false),
                        new CdmColumn("right_id", CdmType.INTEGER, false, false)),
                List.of(
                        new CdmForeignKey("left_id", "lefts", "id"),
                        new CdmForeignKey("right_id", "rights", "id")),
                List.of());

        List<String> statements = PostgresDdlBuilder.addForeignKeyStatements(entity);

        assertThat(statements).hasSize(2);
        assertThat(statements.get(0)).contains("\"fk_junction_left_id\"").contains("REFERENCES \"lefts\" (\"id\")");
        assertThat(statements.get(1)).contains("\"fk_junction_right_id\"").contains("REFERENCES \"rights\" (\"id\")");
    }

    @Test
    void generatedConstraintNameIsTruncatedToSixtyThreeCharactersForVeryLongEntityAndColumnNames() {
        String longEntityName = "a".repeat(40);
        String longColumnName = "b".repeat(40);
        CdmEntity entity = new CdmEntity(
                longEntityName,
                List.of(
                        new CdmColumn("id", CdmType.INTEGER, false, true),
                        new CdmColumn(longColumnName, CdmType.INTEGER, true, false)),
                List.of(new CdmForeignKey(longColumnName, "other", "id")),
                List.of());

        String statement = PostgresDdlBuilder.addForeignKeyStatements(entity).get(0);

        String expectedRawName = "fk_" + longEntityName + "_" + longColumnName;
        assertThat(expectedRawName.length()).isGreaterThan(63);
        String expectedTruncated = expectedRawName.substring(0, 63);
        assertThat(statement).contains("ADD CONSTRAINT \"" + expectedTruncated + "\"");
    }
}
