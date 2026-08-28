package com.dbforge.common.testing.containers;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pinned, reusable MySQL fixture, mirroring {@link DbforgePostgresContainer}.
 * Unlike Postgres, collation is not set at the container level here - hard
 * rule #9's pinned MySQL collation ({@code utf8mb4_bin}) is applied by
 * adapter-mysql's own DDL at database/column-creation time (see
 * {@code MySqlDdlBuilder}/{@code MySqlEngineAdapter}), the same division of
 * responsibility {@link DbforgePostgresContainer} already has with
 * adapter-postgres's {@code C} collation - the container just needs to run
 * a real, unmodified MySQL server.
 */
public final class DbforgeMySqlContainer extends MySQLContainer<DbforgeMySqlContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("mysql:8.0");

    public DbforgeMySqlContainer() {
        super(IMAGE);
        withDatabaseName("dbforge");
        withUsername("dbforge");
        withPassword("dbforge");
        withReuse(true);
    }

    public static DbforgeMySqlContainer defaultInstance() {
        return new DbforgeMySqlContainer();
    }
}
