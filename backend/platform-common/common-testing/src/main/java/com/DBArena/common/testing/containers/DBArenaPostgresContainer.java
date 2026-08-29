package com.dbforge.common.testing.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pinned, reusable Postgres fixture. Collation is {@code C} to match hard
 * rule #9 ("collations are pinned") - a test against the default locale
 * collation would pass locally and fail in CI on a different base image.
 */
public final class DbforgePostgresContainer extends PostgreSQLContainer<DbforgePostgresContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16-alpine");

    public DbforgePostgresContainer() {
        super(IMAGE);
        withDatabaseName("dbforge");
        withUsername("dbforge");
        withPassword("dbforge");
        withReuse(true);
    }

    public static DbforgePostgresContainer defaultInstance() {
        return new DbforgePostgresContainer();
    }
}
