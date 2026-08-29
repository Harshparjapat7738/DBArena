package com.DBArena.common.testing.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pinned, reusable Postgres fixture. Collation is {@code C} to match hard
 * rule #9 ("collations are pinned") - a test against the default locale
 * collation would pass locally and fail in CI on a different base image.
 */
public final class DBArenaPostgresContainer extends PostgreSQLContainer<DBArenaPostgresContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16-alpine");

    public DBArenaPostgresContainer() {
        super(IMAGE);
        withDatabaseName("DBArena");
        withUsername("DBArena");
        withPassword("DBArena");
        withReuse(true);
    }

    public static DBArenaPostgresContainer defaultInstance() {
        return new DBArenaPostgresContainer();
    }
}
