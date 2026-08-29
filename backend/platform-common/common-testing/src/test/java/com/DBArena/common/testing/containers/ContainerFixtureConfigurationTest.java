package com.DBArena.common.testing.containers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks fixture configuration only (pinned image, exposed port) - does
 * NOT start any container, so this suite runs without a Docker daemon.
 * Actual container lifecycle is exercised by the adapter/service tests
 * that use these fixtures against real Docker (B04, B05, B07+).
 */
class ContainerFixtureConfigurationTest {

    @Test
    void postgresPinsImageAndCredentials() {
        DBArenaPostgresContainer container = DBArenaPostgresContainer.defaultInstance();
        assertThat(container.getDockerImageName()).isEqualTo("postgres:16-alpine");
        assertThat(container.getDatabaseName()).isEqualTo("DBArena");
    }

    @Test
    void mongoPinsImage() {
        DBArenaMongoContainer container = DBArenaMongoContainer.defaultInstance();
        assertThat(container.getDockerImageName()).isEqualTo("mongo:7.0");
    }

    @Test
    void redisExposesTheStandardPort() {
        DBArenaRedisContainer container = DBArenaRedisContainer.defaultInstance();
        assertThat(container.getExposedPorts()).containsExactly(6379);
    }

    @Test
    void redpandaPinsImage() {
        DBArenaRedpandaContainer container = DBArenaRedpandaContainer.defaultInstance();
        assertThat(container.getDockerImageName()).isEqualTo("redpandadata/redpanda:v24.2.7");
    }
}
