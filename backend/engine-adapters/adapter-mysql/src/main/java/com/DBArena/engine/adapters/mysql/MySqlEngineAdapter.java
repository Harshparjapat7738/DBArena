package com.DBArena.engine.adapters.mysql;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.UlidIdGenerator;
import com.DBArena.common.core.value.CdmValue;
import com.DBArena.engine.spi.DatabaseEngineAdapter;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.engine.spi.cdm.CdmType;
import com.DBArena.engine.spi.model.ColumnMeta;
import com.DBArena.engine.spi.model.ExecutionError;
import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.engine.spi.model.ExplainPlan;
import com.DBArena.engine.spi.model.MaterializationResult;
import com.DBArena.engine.spi.model.ResultRow;
import com.DBArena.engine.spi.model.SchemaSnapshot;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.engine.spi.model.StatementRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The MySQL implementation of {@link DatabaseEngineAdapter}, built outside
 * the B01-B19 milestone table's numeric order on the human's explicit
 * instruction - see backend/CLAUDE.md's Session Log entry for this class
 * for why. Mirrors {@code PostgresEngineAdapter} (B04) exactly in scope and
 * shape - same five delegate collaborators pattern
 * ({@link MySqlConnectionFactory}, {@link MySqlMaterializer}, {@link
 * MySqlIntrospector}, {@link MySqlTemplateCloner}, and this class itself),
 * same "no connection pooling, no Spring" posture (hard rule #1; see
 * {@link MySqlConnectionFactory}'s Javadoc for why pooling stays out), same
 * "execute/explain run whatever StatementRequest they are given as-is, no
 * AST validation here" contract (hard rule #4 places that strictly
 * upstream, per {@link StatementRequest}'s own Javadoc - this adapter has
 * no dependency on B07/B08 and none should ever be added here, see
 * backend/CLAUDE.md "Adding a new engine").
 *
 * <p>The one structurally different method is {@link #templateClone} -
 * MySQL has no {@code CREATE DATABASE ... TEMPLATE} primitive, so cloning
 * is a real multi-statement operation delegated to {@link
 * MySqlTemplateCloner} rather than one DDL statement. See that class's
 * Javadoc.
 */
public final class MySqlEngineAdapter implements DatabaseEngineAdapter {

    private static final String DATABASE_NAME_PREFIX = "DBArena_";

    private final MySqlConnectionFactory connectionFactory;
    private final MySqlMaterializer materializer;
    private final MySqlIntrospector introspector;
    private final MySqlTemplateCloner cloner;
    private final IdGenerator idGenerator;

    public MySqlEngineAdapter(MySqlConnectionFactory connectionFactory) {
        this(connectionFactory, new UlidIdGenerator());
    }

    public MySqlEngineAdapter(MySqlConnectionFactory connectionFactory, IdGenerator idGenerator) {
        this.connectionFactory = connectionFactory;
        this.materializer = new MySqlMaterializer();
        this.introspector = new MySqlIntrospector();
        this.cloner = new MySqlTemplateCloner();
        this.idGenerator = idGenerator;
    }

    @Override
    public EngineType engineType() {
        return EngineType.MYSQL;
    }

    @Override
    public MaterializationResult materialize(CdmDataset dataset) {
        String databaseName = newDatabaseName();
        createFreshDatabase(databaseName);

        java.util.Map<String, Long> rowCounts;
        try (Connection connection = connectionFactory.connectTo(databaseName)) {
            rowCounts = materializer.materialize(connection, dataset);
        } catch (SQLException e) {
            throw new MySqlAdapterException(
                    "Failed to close connection after materializing '" + databaseName + "'", e);
        }

        SessionHandle session = new SessionHandle(databaseName, EngineType.MYSQL, databaseName);
        return new MaterializationResult(session, Instant.now(), rowCounts);
    }

    @Override
    public SessionHandle templateClone(SessionHandle templateSession) {
        requireMySql(templateSession);
        String newDatabaseName = newDatabaseName();
        try (Connection admin = connectionFactory.adminConnection()) {
            createFreshDatabase(admin, newDatabaseName);
            cloner.clone(admin, templateSession.connectionRef(), newDatabaseName);
        } catch (SQLException e) {
            throw new MySqlAdapterException(
                    "Failed to close connection after cloning template database '"
                            + templateSession.connectionRef() + "'", e);
        }
        return new SessionHandle(newDatabaseName, EngineType.MYSQL, newDatabaseName);
    }

    @Override
    public SchemaSnapshot introspect(SessionHandle session) {
        requireMySql(session);
        try (Connection connection = connectionFactory.connectTo(session.connectionRef())) {
            return introspector.introspect(connection, session.connectionRef());
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to introspect session '" + session.sessionId() + "'", e);
        }
    }

    @Override
    public ExecutionResult execute(SessionHandle session, StatementRequest request) {
        requireMySql(session);
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
        requireMySql(session);
        // FORMAT=JSON: a single row, single JSON column, unlike traditional EXPLAIN's
        // multi-column tabular output - one coherent plan string with no column-joining
        // needed, the same end result PostgresEngineAdapter#explain gets by joining
        // Postgres's one-text-column-per-row output.
        try (Connection connection = connectionFactory.connectTo(session.connectionRef());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("EXPLAIN FORMAT=JSON " + request.statementText())) {
            StringBuilder plan = new StringBuilder();
            while (resultSet.next()) {
                if (!plan.isEmpty()) {
                    plan.append('\n');
                }
                plan.append(resultSet.getString(1));
            }
            return new ExplainPlan(plan.isEmpty() ? "(no plan returned)" : plan.toString(), Optional.empty());
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to explain statement", e);
        }
    }

    @Override
    public void release(SessionHandle session) {
        requireMySql(session);
        // No Postgres-style "WITH (FORCE)" equivalent needed: MySQL's DROP DATABASE
        // succeeds even while other sessions have the database selected as their
        // current schema - it does not lock or refuse based on live connections.
        try (Connection admin = connectionFactory.adminConnection();
             Statement statement = admin.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + MySqlIdentifiers.quote(session.connectionRef()));
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to release session '" + session.sessionId() + "'", e);
        }
    }

    private void createFreshDatabase(String databaseName) {
        try (Connection admin = connectionFactory.adminConnection()) {
            createFreshDatabase(admin, databaseName);
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to close connection after creating database '" + databaseName + "'", e);
        }
    }

    /** Hard rule #9's pinned MySQL collation. Shared by {@link #materialize} (fresh database) and {@link #templateClone} (the clone's target database) - both need an identically-collated, empty schema to start from. */
    private void createFreshDatabase(Connection admin, String databaseName) {
        try (Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + MySqlIdentifiers.quote(databaseName)
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_bin");
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to create database '" + databaseName + "'", e);
        }
    }

    private ExecutionResult readResultSet(ResultSet resultSet, long elapsedMillis) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<ColumnMeta> columns = new ArrayList<>(columnCount);
        List<CdmType> columnTypes = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            // A native type outside MySqlNativeTypes' six known variants (an arbitrary learner
            // query can touch any MySQL feature) falls back to CdmType.TEXT - MySqlValueJdbcCodec's
            // TEXT case reads via ResultSet.getString(), which coerces almost any SQL type
            // reasonably, rather than this method throwing on a type the CDM doesn't model yet.
            CdmType cdmType = MySqlNativeTypes.fromDriverType(
                            metaData.getColumnTypeName(i).toUpperCase(Locale.ROOT), metaData.getPrecision(i))
                    .orElse(CdmType.TEXT);
            columnTypes.add(cdmType);
            boolean nullable = metaData.isNullable(i) != ResultSetMetaData.columnNoNulls;
            columns.add(new ColumnMeta(metaData.getColumnLabel(i), cdmType.valueClass().getSimpleName(), nullable));
        }

        List<ResultRow> rows = new ArrayList<>();
        while (resultSet.next()) {
            List<CdmValue> values = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                values.add(MySqlValueJdbcCodec.read(resultSet, i, columnTypes.get(i - 1)));
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
        return state == null || state.isBlank() ? "mysql.error" : "mysql." + state;
    }

    private String newDatabaseName() {
        return DATABASE_NAME_PREFIX + idGenerator.next().toLowerCase(Locale.ROOT);
    }

    private static void requireMySql(SessionHandle session) {
        if (session.engineType() != EngineType.MYSQL) {
            throw new IllegalArgumentException(
                    "MySqlEngineAdapter cannot operate on a " + session.engineType() + " session");
        }
    }
}
