package com.DBArena.services.execution.sandbox;

import com.DBArena.engine.adapters.postgres.PostgresConnectionFactory;
import com.DBArena.engine.spi.DatabaseEngineAdapter;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.engine.spi.model.ExplainPlan;
import com.DBArena.engine.spi.model.MaterializationResult;
import com.DBArena.engine.spi.model.SchemaSnapshot;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.engine.spi.model.StatementRequest;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.engine.DatabaseEngine;
import com.DBArena.services.execution.materializer.DatasetMaterializer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresSandboxProviderTest {

    private static final ExecutionPolicy POLICY = new ExecutionPolicy(
            5000, 500, 1_048_576, Duration.ofSeconds(5), Duration.ofSeconds(3), 2, 8, 3);

    /**
     * A real {@link PostgresConnectionFactory} pointed at a port nothing
     * listens on - exercises {@code applyConnectionLimit}'s actual failure
     * path (a real connection attempt, really failing) rather than mocking
     * a final class, and proves the fix in this same session: a connection
     * failure there must not propagate and fail an otherwise-valid
     * {@code acquire()} call.
     */
    private static PostgresConnectionFactory unreachableConnectionFactory() {
        return new PostgresConnectionFactory("127.0.0.1", 1, "nobody", "nopass");
    }

    @Test
    void acquireRejectsASessionOutsideTheSandboxNamingConvention() {
        DatasetMaterializer materializer = (engine, slug) -> new SessionHandle("s1", EngineType.POSTGRES, "production");
        PostgresSandboxProvider provider = new PostgresSandboxProvider(materializer, fakeDatabaseEngine(null), unreachableConnectionFactory());

        assertThatThrownBy(() -> provider.acquire(EngineType.POSTGRES, "two-sum", POLICY))
                .isInstanceOf(NotASandboxDatabaseException.class);
    }

    @Test
    void acquireSucceedsForACorrectlyNamedSessionEvenWhenTheConnectionLimitStepFails() {
        DatasetMaterializer materializer = (engine, slug) -> new SessionHandle("s1", EngineType.POSTGRES, "DBArena_abc123");
        PostgresSandboxProvider provider = new PostgresSandboxProvider(materializer, fakeDatabaseEngine(null), unreachableConnectionFactory());

        assertThatCode(() -> {
            SessionHandle session = provider.acquire(EngineType.POSTGRES, "two-sum", POLICY);
            assertThat(session.connectionRef()).isEqualTo("DBArena_abc123");
        }).doesNotThrowAnyException();
    }

    @Test
    void releaseDelegatesToTheResolvedAdapterAndRejectsANonSandboxDatabase() {
        AtomicReference<SessionHandle> released = new AtomicReference<>();
        DatabaseEngine engine = fakeDatabaseEngine(released);
        DatasetMaterializer materializer = (e, slug) -> {
            throw new UnsupportedOperationException("not needed for this test");
        };
        PostgresSandboxProvider provider = new PostgresSandboxProvider(materializer, engine, unreachableConnectionFactory());

        SessionHandle good = new SessionHandle("s1", EngineType.POSTGRES, "DBArena_abc123");
        provider.release(good);
        assertThat(released.get()).isEqualTo(good);

        SessionHandle bad = new SessionHandle("s2", EngineType.POSTGRES, "production");
        assertThatThrownBy(() -> provider.release(bad)).isInstanceOf(NotASandboxDatabaseException.class);
    }

    private static DatabaseEngine fakeDatabaseEngine(AtomicReference<SessionHandle> releasedCapture) {
        return type -> new DatabaseEngineAdapter() {
            @Override
            public EngineType engineType() {
                return EngineType.POSTGRES;
            }

            @Override
            public MaterializationResult materialize(CdmDataset dataset) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SessionHandle templateClone(SessionHandle templateSession) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SchemaSnapshot introspect(SessionHandle session) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ExecutionResult execute(SessionHandle session, StatementRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ExplainPlan explain(SessionHandle session, StatementRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void release(SessionHandle session) {
                if (releasedCapture != null) {
                    releasedCapture.set(session);
                }
            }
        };
    }
}
