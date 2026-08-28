package com.dbforge.common.testing.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Pinned, reusable Redis fixture. No dedicated Testcontainers Redis module exists, hence GenericContainer. */
public final class DbforgeRedisContainer extends GenericContainer<DbforgeRedisContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final int REDIS_PORT = 6379;

    public DbforgeRedisContainer() {
        super(IMAGE);
        withExposedPorts(REDIS_PORT);
        withReuse(true);
    }

    public String connectionUrl() {
        return "redis://" + getHost() + ":" + getMappedPort(REDIS_PORT);
    }

    public static DbforgeRedisContainer defaultInstance() {
        return new DbforgeRedisContainer();
    }
}
