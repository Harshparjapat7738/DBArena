package com.DBArena.services.execution.sandbox;

import com.DBArena.engine.adapters.postgres.PostgresConnectionFactory;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.engine.DatabaseEngine;
import com.DBArena.services.execution.materializer.DatasetMaterializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class PostgresSandboxProvider implements SandboxProvider {

    private static final Logger log = LoggerFactory.getLogger(PostgresSandboxProvider.class);

    /**
     * Mirrors {@code PostgresEngineAdapter}'s own {@code DATABASE_NAME_PREFIX} -
     * that constant is private to its module (engine-adapters must not expose
     * execution-service-specific concerns), so this is intentionally a
     * second, independent literal, not a shared constant - see
     * {@link NotASandboxDatabaseException}'s Javadoc for why this check
     * exists at all.
     */
    private static final String SANDBOX_DATABASE_PREFIX = "DBArena_";

    private final DatasetMaterializer datasetMaterializer;
    private final DatabaseEngine databaseEngine;
    private final PostgresConnectionFactory connectionFactory;

    public PostgresSandboxProvider(
            DatasetMaterializer datasetMaterializer, DatabaseEngine databaseEngine, PostgresConnectionFactory connectionFactory) {
        this.datasetMaterializer = datasetMaterializer;
        this.databaseEngine = databaseEngine;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public SessionHandle acquire(EngineType engine, String datasetSlug, ExecutionPolicy policy) {
        SessionHandle session = datasetMaterializer.acquireFreshSession(engine, datasetSlug);
        assertIsASandboxDatabase(session);
        if (engine == EngineType.POSTGRES) {
            applyConnectionLimit(session, policy.sandboxConnectionLimit());
        }
        return session;
    }

    @Override
    public void release(SessionHandle session) {
        assertIsASandboxDatabase(session);
        databaseEngine.resolve(session.engineType()).release(session);
    }

    /** Defense in depth, in addition to the service-level concurrency semaphore ({@code ExecutionService}) - caps how many live connections Postgres itself will accept to this one disposable database. */
    private void applyConnectionLimit(SessionHandle session, int limit) {
        try (Connection admin = connectionFactory.adminConnection();
             Statement statement = admin.createStatement()) {
            statement.execute("ALTER DATABASE \"" + session.connectionRef() + "\" CONNECTION LIMIT " + limit);
        } catch (SQLException | RuntimeException e) {
            // Not fatal to the execution itself - the service-level semaphore is the primary
            // control; log and continue rather than failing an otherwise-valid session over this.
            // RuntimeException here is deliberate, not a catch-all habit: PostgresConnectionFactory
            // wraps a connection failure as its own unchecked PostgresAdapterException, not SQLException.
            log.warn("Could not apply connection limit to sandbox database {}: {}", session.connectionRef(), e.getMessage());
        }
    }

    private static void assertIsASandboxDatabase(SessionHandle session) {
        if (session.engineType() == EngineType.POSTGRES && !session.connectionRef().startsWith(SANDBOX_DATABASE_PREFIX)) {
            throw new NotASandboxDatabaseException(session.connectionRef());
        }
    }
}
