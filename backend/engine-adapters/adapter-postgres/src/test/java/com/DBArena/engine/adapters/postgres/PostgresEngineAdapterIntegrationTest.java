package com.dbforge.engine.adapters.postgres;

import com.dbforge.common.core.value.CdmValue;
import com.dbforge.common.testing.containers.DbforgePostgresContainer;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.model.ColumnMeta;
import com.dbforge.engine.spi.model.EntitySchema;
import com.dbforge.engine.spi.model.ExecutionResult;
import com.dbforge.engine.spi.model.ExplainPlan;
import com.dbforge.engine.spi.model.MaterializationResult;
import com.dbforge.engine.spi.model.ResultRow;
import com.dbforge.engine.spi.model.SchemaSnapshot;
import com.dbforge.engine.spi.model.SessionHandle;
import com.dbforge.engine.spi.model.StatementRequest;
import com.dbforge.tools.datasetcli.CdmDatasetLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-lifecycle test of {@link PostgresEngineAdapter} against a real
 * Postgres container (hard rule #3: never mock a database). Deliberately
 * plain JUnit5 with {@code @Testcontainers}/{@code @Container} only - no
 * Spring annotation anywhere, matching {@link PostgresAdapterArchitectureTest}'s
 * enforcement that this module never depends on Spring.
 *
 * <p>Uses the real {@code datasets/two-sum} fixture (loaded through
 * dataset-cli's {@link CdmDatasetLoader}, the same code path a real
 * ingestion flow would use) rather than a hand-built {@code CdmDataset},
 * so this proves the adapter against the dataset every other tool in this
 * repo already treats as the canonical end-to-end fixture - see that
 * file's own header comment.
 */
@Testcontainers
class PostgresEngineAdapterIntegrationTest {

    @Container
    static final DbforgePostgresContainer POSTGRES = new DbforgePostgresContainer();

    private static final Path TWO_SUM_DATASET_PATH = Path.of("../../../datasets/two-sum/dataset.yaml");

    private static CdmDataset twoSum;
    private static PostgresConnectionFactory connectionFactory;

    private PostgresEngineAdapter adapter;

    @BeforeAll
    static void loadFixtureAndConnectionFactory() throws IOException {
        twoSum = CdmDatasetLoader.load(TWO_SUM_DATASET_PATH);
        connectionFactory = new PostgresConnectionFactory(
                POSTGRES.getHost(), POSTGRES.getMappedPort(5432), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @BeforeEach
    void newAdapter() {
        adapter = new PostgresEngineAdapter(connectionFactory);
    }

    @Test
    void engineTypeIsPostgres() {
        assertThat(adapter.engineType()).isEqualTo(com.dbforge.engine.spi.EngineType.POSTGRES);
    }

    @Test
    void fullAdapterLifecycle_materializeIntrospectExecuteExplainCloneAndRelease() throws SQLException {
        // --- materialize -----------------------------------------------------------------
        MaterializationResult materialization = adapter.materialize(twoSum);
        SessionHandle session = materialization.session();

        assertThat(session.engineType()).isEqualTo(com.dbforge.engine.spi.EngineType.POSTGRES);
        assertThat(session.connectionRef()).startsWith("dbforge_");
        assertThat(materialization.rowCountsByEntity())
                .isEqualTo(Map.of("numbers", 4L, "queries", 3L));

        // --- introspect --------------------------------------------------------------------
        SchemaSnapshot schema = adapter.introspect(session);
        assertThat(schema.entities()).hasSize(2);

        EntitySchema numbersSchema = schema.entities().stream()
                .filter(e -> e.name().equals("numbers")).findFirst().orElseThrow();
        assertThat(numbersSchema.columns()).extracting(ColumnMeta::name)
                .containsExactly("id", "value", "weight", "label", "is_active");
        assertThat(columnNamed(numbersSchema, "id").nullable()).isFalse();
        assertThat(columnNamed(numbersSchema, "id").cdmTypeName()).isEqualTo("Int");
        assertThat(columnNamed(numbersSchema, "weight").nullable()).isTrue();
        assertThat(columnNamed(numbersSchema, "weight").cdmTypeName()).isEqualTo("Decimal");
        assertThat(columnNamed(numbersSchema, "label").cdmTypeName()).isEqualTo("Text");
        assertThat(columnNamed(numbersSchema, "is_active").cdmTypeName()).isEqualTo("Bool");

        EntitySchema queriesSchema = schema.entities().stream()
                .filter(e -> e.name().equals("queries")).findFirst().orElseThrow();
        assertThat(columnNamed(queriesSchema, "created_at").cdmTypeName()).isEqualTo("Timestamp");
        assertThat(columnNamed(queriesSchema, "created_at").nullable()).isFalse();
        assertThat(columnNamed(queriesSchema, "metadata").cdmTypeName()).isEqualTo("Json");
        assertThat(columnNamed(queriesSchema, "metadata").nullable()).isTrue();
        assertThat(columnNamed(queriesSchema, "matched_number_id").nullable()).isTrue();

        // --- execute: a non-null row, exercising every CdmType except JSON's null case -----
        ExecutionResult numbersResult = adapter.execute(session,
                new StatementRequest("SELECT id, value, weight, label, is_active FROM numbers ORDER BY id", Duration.ofSeconds(5)));
        assertThat(numbersResult.isSuccess()).isTrue();
        assertThat(numbersResult.rows()).hasSize(4);

        ResultRow firstNumberRow = numbersResult.rows().get(0);
        assertThat(((CdmValue.Int) firstNumberRow.values().get(0)).value()).isEqualTo(1L);
        assertThat(((CdmValue.Int) firstNumberRow.values().get(1)).value()).isEqualTo(2L);
        assertThat(((CdmValue.Decimal) firstNumberRow.values().get(2)).toBigDecimal())
                .isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(((CdmValue.Text) firstNumberRow.values().get(3)).value()).isEqualTo("a");
        assertThat(((CdmValue.Bool) firstNumberRow.values().get(4)).value()).isTrue();

        // --- execute: the row with every nullable column actually null ---------------------
        ResultRow thirdNumberRow = numbersResult.rows().get(2);
        assertThat(thirdNumberRow.values().get(2)).isInstanceOf(CdmValue.Null.class);
        assertThat(thirdNumberRow.values().get(3)).isInstanceOf(CdmValue.Null.class);
        assertThat(((CdmValue.Bool) thirdNumberRow.values().get(4)).value()).isFalse();

        // --- execute: TIMESTAMP and JSON columns, plus a nullable foreign key ---------------
        ExecutionResult queriesResult = adapter.execute(session,
                new StatementRequest(
                        "SELECT id, matched_number_id, created_at, metadata FROM queries ORDER BY id",
                        Duration.ofSeconds(5)));
        assertThat(queriesResult.isSuccess()).isTrue();
        ResultRow firstQueryRow = queriesResult.rows().get(0);
        assertThat(((CdmValue.Int) firstQueryRow.values().get(1)).value()).isEqualTo(1L);
        assertThat(((CdmValue.Timestamp) firstQueryRow.values().get(2)).epochMillis())
                .isEqualTo(java.time.Instant.parse("2026-01-01T00:00:00Z").toEpochMilli());
        assertThat(((CdmValue.Json) firstQueryRow.values().get(3)).canonicalJson()).contains("\"source\":\"seed\"");

        ResultRow thirdQueryRow = queriesResult.rows().get(2);
        assertThat(thirdQueryRow.values().get(1)).isInstanceOf(CdmValue.Null.class); // matched_number_id is null

        // --- execute: a statement that fails is data, not an exception ---------------------
        ExecutionResult failed = adapter.execute(session,
                new StatementRequest("SELECT * FROM no_such_table", Duration.ofSeconds(5)));
        assertThat(failed.isSuccess()).isFalse();
        assertThat(failed.error()).isPresent();
        assertThat(failed.error().orElseThrow().code()).startsWith("postgres.");

        // --- explain -------------------------------------------------------------------------
        ExplainPlan plan = adapter.explain(session, new StatementRequest("SELECT * FROM numbers", Duration.ofSeconds(5)));
        assertThat(plan.rawPlanText()).isNotBlank();

        // --- templateClone: an independent copy, not a live view of the original -----------
        SessionHandle clone = adapter.templateClone(session);
        assertThat(clone.connectionRef()).isNotEqualTo(session.connectionRef());

        ExecutionResult cloneRowsBeforeWrite = adapter.execute(clone,
                new StatementRequest("SELECT count(*) FROM numbers", Duration.ofSeconds(5)));
        assertThat(((CdmValue.Int) cloneRowsBeforeWrite.rows().get(0).values().get(0)).value()).isEqualTo(4L);

        ExecutionResult insertIntoClone = adapter.execute(clone,
                new StatementRequest(
                        "INSERT INTO numbers (id, value, weight, label, is_active) VALUES (99, 1, 1, 'x', true)",
                        Duration.ofSeconds(5)));
        assertThat(insertIntoClone.isSuccess()).isTrue();

        ExecutionResult originalRowsAfterCloneWrite = adapter.execute(session,
                new StatementRequest("SELECT count(*) FROM numbers", Duration.ofSeconds(5)));
        assertThat(((CdmValue.Int) originalRowsAfterCloneWrite.rows().get(0).values().get(0)).value())
                .isEqualTo(4L); // the clone's insert did not leak back into the original

        // --- release: both databases actually go away ---------------------------------------
        adapter.release(session);
        adapter.release(clone);
        assertThat(databaseExists(session.connectionRef())).isFalse();
        assertThat(databaseExists(clone.connectionRef())).isFalse();
    }

    @Test
    void releaseIsIdempotent() {
        MaterializationResult materialization = adapter.materialize(twoSum);
        SessionHandle session = materialization.session();

        adapter.release(session);
        assertThatCode(() -> adapter.release(session)).doesNotThrowAnyException();
    }

    @Test
    void templateCloneRejectsANonPostgresSessionHandle() {
        SessionHandle mongoSession = new SessionHandle("s1", com.dbforge.engine.spi.EngineType.MONGODB, "ref");

        assertThatThrownBy(() -> adapter.templateClone(mongoSession))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ColumnMeta columnNamed(EntitySchema entity, String columnName) {
        return entity.columns().stream()
                .filter(c -> c.name().equals(columnName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no column '" + columnName + "' in entity '" + entity.name() + "'"));
    }

    private static boolean databaseExists(String databaseName) throws SQLException {
        try (Connection admin = connectionFactory.adminConnection();
             java.sql.Statement statement = admin.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery(
                     "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'")) {
            return resultSet.next();
        }
    }
}
