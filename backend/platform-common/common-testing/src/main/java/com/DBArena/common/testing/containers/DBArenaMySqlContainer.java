package com.DBArena.common.testing.containers;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pinned, reusable MySQL fixture, mirroring {@link DBArenaPostgresContainer}.
 * Unlike Postgres, collation is not set at the container level here - hard
 * rule #9's pinned MySQL collation ({@code utf8mb4_bin}) is applied by
 * adapter-mysql's own DDL at database/column-creation time (see
 * {@code MySqlDdlBuilder}/{@code MySqlEngineAdapter}), the same division of
 * responsibility {@link DBArenaPostgresContainer} already has with
 * adapter-postgres's {@code C} collation - the container just needs to run
 * a real, unmodified MySQL server.
 */
public final class DBArenaMySqlContainer extends MySQLContainer<DBArenaMySqlContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("mysql:8.0");

    public DBArenaMySqlContainer() {
        super(IMAGE);
        withDatabaseName("DBArena");
        withUsername("DBArena");
        withPassword("DBArena");
        withReuse(true);
    }

    public static DBArenaMySqlContainer defaultInstance() {
        return new DBArenaMySqlContainer();
    }
}
