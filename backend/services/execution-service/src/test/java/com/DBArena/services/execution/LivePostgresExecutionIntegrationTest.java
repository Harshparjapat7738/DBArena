package com.DBArena.services.execution;

import com.DBArena.engine.adapters.postgres.PostgresConnectionFactory;
import com.DBArena.engine.adapters.postgres.PostgresEngineAdapter;
import com.DBArena.engine.spi.DatabaseEngineAdapter;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.config.ExecutionProperties;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.engine.DatabaseEngine;
import com.DBArena.services.execution.evaluator.DefaultResultEvaluator;
import com.DBArena.services.execution.executor.DefaultQueryExecutor;
import com.DBArena.services.execution.executor.QueryExecutionOutcome;
import com.DBArena.services.execution.materializer.CdmDatasetMaterializer;
import com.DBArena.services.execution.materializer.DatasetMaterializer;
import com.DBArena.services.execution.sandbox.PostgresSandboxProvider;
import com.DBArena.services.execution.sandbox.SandboxProvider;
import com.DBArena.services.execution.validation.PostgresSqlQueryValidator;
import com.DBArena.services.execution.validation.ValidationResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Actual PostgreSQL execution verification" (the B04 task brief), against
 * the real local Postgres 18 install this session found running (no
 * Docker/Testcontainers in this environment - see CLAUDE.md's B04 Session
 * Log entry) - a real materialize -&gt; sandbox -&gt; validate -&gt; execute ->
 * evaluate round trip through the actual two-sum CDM dataset, plus proof
 * that a malicious statement never reaches Postgres at all.
 *
 * <p>Skipped entirely (not failed) when {@code DBArena_EXECUTION_POSTGRES_PASSWORD}
 * isn't set - same "environment-gated, not a stub" posture as every other
 * live-infrastructure test in this repo (Testcontainers-backed tests are
 * gated on Docker being present the same way).
 */
@EnabledIfEnvironmentVariable(named = "DBArena_EXECUTION_POSTGRES_PASSWORD", matches = ".+")
class LivePostgresExecutionIntegrationTest {

    private static final ExecutionPolicy POLICY = new ExecutionPolicy(
            5000, 500, 1_048_576, Duration.ofSeconds(5), Duration.ofSeconds(3), 2, 8, 3);

    private static PostgresConnectionFactory connectionFactory;
    private static DatabaseEngine databaseEngine;
    private static DatasetMaterializer materializer;
    private static SandboxProvider sandboxProvider;
    private static SessionHandle session;

    @BeforeAll
    static void materializeATemplateOnce() {
        String host = System.getenv().getOrDefault("DBArena_EXECUTION_POSTGRES_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("DBArena_EXECUTION_POSTGRES_PORT", "5432"));
        String username = System.getenv().getOrDefault("DBArena_EXECUTION_POSTGRES_USERNAME", "dbarena_sandbox");
        String password = System.getenv("DBArena_EXECUTION_POSTGRES_PASSWORD");

        connectionFactory = new PostgresConnectionFactory(host, port, username, password);
        DatabaseEngineAdapter adapter = new PostgresEngineAdapter(connectionFactory);
        databaseEngine = type -> adapter;

        ExecutionProperties properties = new ExecutionProperties();
        properties.setDatasetsRoot("../../../datasets");
        materializer = new CdmDatasetMaterializer(databaseEngine, properties);
        sandboxProvider = new PostgresSandboxProvider(materializer, databaseEngine, connectionFactory);

        session = sandboxProvider.acquire(EngineType.POSTGRES, "two-sum", POLICY);
    }

    @AfterAll
    static void releaseTheSession() {
        if (session != null) {
            sandboxProvider.release(session);
        }
    }

    @Test
    void realSelectAgainstTheRealMaterializedTwoSumDatasetReturnsRealRows() {
        PostgresSqlQueryValidator validator = new PostgresSqlQueryValidator();
        String sql = "SELECT id, value, is_active FROM numbers ORDER BY id";

        ValidationResult validation = validator.validate(sql, POLICY);
        assertThat(validation.allowed()).as("validator should allow a plain SELECT").isTrue();

        DefaultQueryExecutor executor = new DefaultQueryExecutor(databaseEngine, new DefaultResultEvaluator());
        QueryExecutionOutcome outcome = executor.execute(session, EngineType.POSTGRES, sql, POLICY);

        assertThat(outcome.isSuccess()).as("engine error: %s", outcome.raw().error()).isTrue();
        var evaluation = outcome.evaluation().orElseThrow();
        assertThat(evaluation.summary().rows()).isNotEmpty();
        assertThat(evaluation.summary().columns()).extracting(c -> c.name()).contains("id", "value", "is_active");
        assertThat(evaluation.metrics().rowsReturned()).isEqualTo(evaluation.summary().rows().size());
        assertThat(evaluation.metrics().executionTimeMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void aMaliciousStatementIsRejectedByTheValidatorAndNeverReachesPostgres() {
        PostgresSqlQueryValidator validator = new PostgresSqlQueryValidator();
        ValidationResult validation = validator.validate("DROP TABLE numbers", POLICY);

        assertThat(validation.allowed()).isFalse();

        // Prove it really wasn't dropped - the very next real SELECT against the same
        // session still works.
        DefaultQueryExecutor executor = new DefaultQueryExecutor(databaseEngine, new DefaultResultEvaluator());
        QueryExecutionOutcome outcome = executor.execute(session, EngineType.POSTGRES, "SELECT COUNT(*) FROM numbers", POLICY);
        assertThat(outcome.isSuccess()).isTrue();
    }

    @Test
    void rowLimitIsEnforcedAgainstRealData() {
        ExecutionPolicy tightPolicy = new ExecutionPolicy(5000, 1, 1_048_576, Duration.ofSeconds(5), Duration.ofSeconds(3), 2, 8, 3);
        DefaultQueryExecutor executor = new DefaultQueryExecutor(databaseEngine, new DefaultResultEvaluator());

        QueryExecutionOutcome outcome = executor.execute(session, EngineType.POSTGRES, "SELECT * FROM numbers", tightPolicy);

        assertThat(outcome.isSuccess()).isTrue();
        var evaluation = outcome.evaluation().orElseThrow();
        assertThat(evaluation.summary().rows()).hasSize(1);
        assertThat(evaluation.summary().truncated()).isTrue();
    }

    @Test
    void explainProducesARealPostgresPlan() {
        var explainProvider = new com.DBArena.services.execution.explain.DefaultExplainProvider(databaseEngine);
        var plan = explainProvider.explain(session, EngineType.POSTGRES, "SELECT * FROM numbers", POLICY);

        assertThat(plan.rawPlanText()).containsIgnoringCase("numbers");
    }
}
