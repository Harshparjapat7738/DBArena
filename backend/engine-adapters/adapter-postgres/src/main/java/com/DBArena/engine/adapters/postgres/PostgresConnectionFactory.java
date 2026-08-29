package com.DBArena.engine.adapters.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens JDBC connections to a Postgres server: either the fixed
 * maintenance database (for {@code CREATE}/{@code DROP DATABASE}, which
 * cannot run against the database being created or dropped itself), or
 * one specific database this adapter has already materialized.
 * Deliberately not a connection pool - one short-lived {@link Connection}
 * per adapter call, closed immediately after. Pooling per-database is a
 * concern for whichever service configures this adapter; owning one here
 * (e.g. HikariCP) would pull a framework dependency into this module,
 * against hard rule #1.
 */
public final class PostgresConnectionFactory {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String maintenanceDatabase;

    public PostgresConnectionFactory(String host, int port, String username, String password) {
        this(host, port, username, password, "postgres");
    }

    public PostgresConnectionFactory(
            String host, int port, String username, String password, String maintenanceDatabase) {
        this.host = require(host, "host");
        this.port = port;
        this.username = require(username, "username");
        this.password = password == null ? "" : password;
        this.maintenanceDatabase = require(maintenanceDatabase, "maintenanceDatabase");
    }

    /** For {@code CREATE DATABASE}/{@code DROP DATABASE} - these must run against a database other than the one being created/dropped. */
    public Connection adminConnection() {
        return connectTo(maintenanceDatabase);
    }

    public Connection connectTo(String databaseName) {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl(databaseName), username, password);
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            throw new PostgresAdapterException("Could not connect to Postgres database '" + databaseName + "'", e);
        }
    }

    private String jdbcUrl(String databaseName) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
