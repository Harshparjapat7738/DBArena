package com.dbforge.engine.adapters.postgres;

import com.dbforge.common.core.id.IdGenerator;
import com.dbforge.common.core.id.UlidIdGenerator;
import com.dbforge.common.core.value.CdmValue;
import com.dbforge.engine.spi.DatabaseEngineAdapter;
import com.dbforge.engine.spi.EngineType;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.cdm.CdmType;
import com.dbforge.engine.spi.model.ColumnMeta;
import com.dbforge.engine.spi.model.ExecutionError;
import com.dbforge.engine.spi.model.ExecutionResult;
import com.dbforge.engine.spi.model.ExplainPlan;
import com.dbforge.engine.spi.model.MaterializationResult;
import com.dbforge.engine.spi.model.ResultRow;
import com.dbforge.engine.spi.model.SchemaSnapshot;
import com.dbforge.engine.spi.model.SessionHandle;
import com.dbforge.engine.spi.model.StatementRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The Postgres implementation of {@link DatabaseEngineAdapter} (B04).
 * Owns per-session database creation/cloning via native
 * {@code CREATE DATABASE ... TEMPLATE ...} and plain-JDBC execution - no
 * connection pooling, no Spring (hard rule #1; see
 * {@link PostgresConnectionFactory}'s Javadoc for why pooling stays out).
 *
 * <p>{@code execute}/{@code explain} run whatever {@link StatementRequest}
 * they are given as-is - no AST validation happens here. That is by
 * design: hard rule #4 places statement validation (B08, not yet built)
 * strictly before a {@code StatementRequest} is ever constructed, per that
 * record's own Javadoc. This adapter has no dependency on B07 or B08 and
 * none should ever be added here - see backend/CLAUDE.md "Adding a new
 * engine": if adding/completing an engine means touching
 * execution-service, the abstraction has leaked.
 */
public final class PostgresEngineAdapter implements DatabaseEngineAdapter {

    private static final String DATABASE_NAME_PREFIX = "dbforge_";

    private final PostgresConnectionFactory connectionFactory;
    private final PostgresMaterializer materializer;
    private final PostgresIntrospector introspector;
    private final IdGenerator idGenerator;

    public PostgresEngineAdapter(PostgresConnectionFactory connectionFactory) {
        this(connectionFactory, new UlidIdGenerator());
    }

    public PostgresEngineAdapter(PostgresConnectionFactory connectionFactory, IdGenerator idGenerator) {
        this.connectionFactory = connectionFactory;
        this.materializer = new PostgresMaterializer();
        this.introspector = new PostgresIntrospector();
        this.idGenerator = idGenerator;
    }

    @Override
    public EngineType engineType() {
        return EngineType.POSTGRES;
    }

    @Override
    public MaterializationResult materialize(CdmDataset dataset) {
        String databaseName = newDatabaseName();
        createFreshDatabase(databaseName);

        Map<String, Long> rowCounts;
        try (Connection connection = connectionFactory.connectTo(databaseName)) {
            rowCounts = materializer.materialize(connection, dataset);
        } catch (SQLException e) {
            throw new PostgresAdapterException(
                    "Failed to close connection after materializing '" + databaseName + "'", e);
        }

        SessionHandle session = new SessionHandle(databaseName, EngineType.POSTGRES, databaseName);
        return new MaterializationResult(session, Instant.now(), rowCounts);
    }

    @Override
    public SessionHandle templateClone(SessionHandle templateSession) {
        requirePostgres(templateSession);
        String newDatabaseName = newDatabaseName();
        try (Connection admin = connectionFactory.adminConnection();
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + PostgresIdentifiers.quote(newDatabaseName)
                    + " TEMPLATE " + PostgresIdentifiers.quote(templateSession.connectionRef()));
        } catch (SQLException e) {
            throw new PostgresAdapterException(
                    "Failed to clone template database '" + templateSession.connectionRef() + "'", e);
        }
        return new SessionHandle(newDatabaseName, EngineType.POSTGRES, newDatabaseName);
    }

    @Override
    public SchemaSnapshot introspect(SessionHandle session) {
        requirePostgres(session);
        try (Connection connection = connectionFactory.connectTo(session.connectionRef())) {
            return introspector.introspect(connection);
        } catch (SQLException e) {
            throw new PostgresAdapterException("Failed to introspect session '" + session.sessionId() + "'", e);
        }
    }

    @Override
    public ExecutionResult execute(SessionHandle session, StatementRequest request) {
        requirePostgres(session);
        long startedAt = System.currentTimeMillis();
        try (Connection connection = connectionFactory.connectTo(session.connectionRef());
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSeconds(request));
            boolean hasResultSet = statement.execute(request.statementText());
            long elapsedMillis = System.currentTimeMillis() - startedAt;

            if (!hasResultSet) {
                return new ExecutionResult(List.of(), List.of(), elapsedMillis, Optional.empty());
            }
            try (ResultSet resultSet = statement.getResultSet()) {
                return readResultSet(resultSet, elapsedMillis);
            }
        } catch (SQLException e) {
            // Per this method's own contract (see DatabaseEngineAdapter's Javadoc): a normal query
            // failure - bad SQL, a constraint violation, a timeout - is data, never an exception here.
            long elapsedMillis = System.currentTimeMillis() - startedAt;
            return ExecutionResult.failure(new ExecutionError(sqlErrorCode(e), e.getMessage()), elapsedMillis);
        }
    }

    @Override
    public ExplainPlan explain(SessionHandle session, StatementRequest request) {
        requirePostgres(session);
        try (Connection connection = connectionFactory.connectTo(session.connectionRef());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("EXPLAIN " + request.statementText())) {
            StringBuilder plan = new StringBuilder();
            while (resultSet.next()) {
                if (!plan.isEmpty()) {
                    plan.append('\n');
                }
                plan.append(resultSet.getString(1));
            }
            return new ExplainPlan(plan.isEmpty() ? "(no plan returned)" : plan.toString(), Optional.empty());
        } catch (SQLException e) {
            throw new PostgresAdapterException("Failed to explain statement", e);
        }
    }

    @Override
    public void release(SessionHandle session) {
        requirePostgres(session);
        try (Connection admin = connectionFactory.adminConnection();
             Statement statement = admin.createStatement()) {
            // WITH (FORCE) (Postgres 13+) disconnects any lingering session on the target
            // database first - release() must be able to actually free the database, not fail
            // because something still holds a connection to it.
            statement.execute("DROP DATABASE IF EXISTS " + PostgresIdentifiers.quote(session.connectionRef())
                    + " WITH (FORCE)");
        } catch (SQLException e) {
            throw new PostgresAdapterException("Failed to release session '" + session.sessionId() + "'", e);
        }
    }

    private void createFreshDatabase(String databaseName) {
        try (Connection admin = connectionFactory.adminConnection();
             Statement statement = admin.createStatement()) {
            // TEMPLATE template0 is required to override locale - template1 (the normal implicit
            // default) cannot have its collation changed by CREATE DATABASE. C is hard rule #9's
            // pinned Postgres collation; every Postgres install ships it, glibc- or musl-based alike.
            statement.execute("CREATE DATABASE " + PostgresIdentifiers.quote(databaseName)
                    + " WITH TEMPLATE template0 ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C'");
        } catch (SQLException e) {
            throw new PostgresAdapterException("Failed to create database '" + databaseName + "'", e);
        }
    }

    private ExecutionResult readResultSet(ResultSet resultSet, long elapsedMillis) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<ColumnMeta> columns = new ArrayList<>(columnCount);
        List<CdmType> columnTypes = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            // A native type outside PostgresNativeTypes' six known variants (an arbitrary learner
            // query can touch any Postgres feature) falls back to CdmType.TEXT - CdmValueJdbcCodec's
            // TEXT case reads via ResultSet.getString(), which coerces almost any SQL type
            // reasonably, rather than this method throwing on a type the CDM doesn't model yet.
            CdmType cdmType = PostgresNativeTypes.toCdmType(metaData.getColumnTypeName(i).toLowerCase(Locale.ROOT))
                    .orElse(CdmType.TEXT);
            columnTypes.add(cdmType);
            boolean nullable = metaData.isNullable(i) != ResultSetMetaData.columnNoNulls;
            columns.add(new ColumnMeta(metaData.getColumnLabel(i), cdmType.valueClass().getSimpleName(), nullable));
        }

        List<ResultRow> rows = new ArrayList<>();
        while (resultSet.next()) {
            List<CdmValue> values = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                values.add(CdmValueJdbcCodec.read(resultSet, i, columnTypes.get(i - 1)));
            }
            rows.add(new ResultRow(values));
        }
        return new ExecutionResult(columns, rows, elapsedMillis, Optional.empty());
    }

    /** Never 0 - JDBC's setQueryTimeout(0) means "no timeout", which a sub-second StatementRequest.timeout must never silently become. */
    private static int timeoutSeconds(StatementRequest request) {
        long millis = request.timeout().toMillis();
        return (int) Math.max(1, Math.ceil(millis / 1000.0));
    }

    private static String sqlErrorCode(SQLException e) {
        String state = e.getSQLState();
        return state == null || state.isBlank() ? "postgres.error" : "postgres." + state;
    }

    private String newDatabaseName() {
        return DATABASE_NAME_PREFIX + idGenerator.next().toLowerCase(Locale.ROOT);
    }

    private static void requirePostgres(SessionHandle session) {
        if (session.engineType() != EngineType.POSTGRES) {
            throw new IllegalArgumentException(
                    "PostgresEngineAdapter cannot operate on a " + session.engineType() + " session");
        }
    }
}
