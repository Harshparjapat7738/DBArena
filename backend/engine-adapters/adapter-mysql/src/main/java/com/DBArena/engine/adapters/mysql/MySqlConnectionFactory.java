package com.DBArena.engine.adapters.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens JDBC connections to a MySQL server: either unscoped to any
 * particular database (for {@code CREATE DATABASE}/{@code DROP DATABASE}
 * and, in {@link MySqlTemplateCloner}, cross-schema {@code CREATE TABLE
 * ... LIKE}/{@code INSERT ... SELECT} statements addressed with fully
 * qualified {@code `schema`.`table`} names) or scoped to one specific
 * database this adapter has already materialized. Unlike Postgres, MySQL
 * does not require a "current" database to exist for {@code CREATE
 * DATABASE} to run, nor does it isolate schemas per connection - a single
 * connection can read/write any schema its user has privileges on via a
 * qualified name - so, unlike {@code PostgresConnectionFactory}, there is
 * no separate "maintenance database" concept here at all.
 *
 * <p>Deliberately not a connection pool - one short-lived {@link
 * Connection} per adapter call, closed immediately after. Pooling
 * per-database is a concern for whichever service configures this
 * adapter; owning one here (e.g. HikariCP) would pull a framework
 * dependency into this module, against hard rule #1.
 */
public final class MySqlConnectionFactory {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public MySqlConnectionFactory(String host, int port, String username, String password) {
        this.host = require(host, "host");
        this.port = port;
        this.username = require(username, "username");
        this.password = password == null ? "" : password;
    }

    /** For {@code CREATE DATABASE}/{@code DROP DATABASE} and cross-schema copy statements - no default schema selected. */
    public Connection adminConnection() {
        return open(null);
    }

    public Connection connectTo(String databaseName) {
        return open(require(databaseName, "databaseName"));
    }

    private Connection open(String databaseName) {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl(databaseName), username, password);
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            throw new MySqlAdapterException(
                    "Could not connect to MySQL"
                            + (databaseName == null ? "" : " database '" + databaseName + "'"), e);
        }
    }

    /**
     * {@code useSSL=false}/{@code allowPublicKeyRetrieval=true} - required
     * against a default MySQL 8 server (caching_sha2_password auth plugin)
     * with no TLS configured, the normal local-dev/Testcontainers case.
     * {@code serverTimezone=UTC} is defense-in-depth only - every
     * TIMESTAMP-typed value this adapter reads/writes goes through
     * {@code MySqlValueJdbcCodec} as a zone-naive {@code LocalDateTime}
     * bound to a {@code DATETIME} column (see {@code MySqlColumnType.DATETIME}'s
     * Javadoc), which Connector/J does not subject to timezone conversion
     * regardless of this setting - but pinning it removes any doubt.
     */
    private String jdbcUrl(String databaseName) {
        String path = databaseName == null ? "" : "/" + databaseName;
        return "jdbc:mysql://" + host + ":" + port + path
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
