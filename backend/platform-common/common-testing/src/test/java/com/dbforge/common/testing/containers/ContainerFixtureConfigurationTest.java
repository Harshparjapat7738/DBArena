package com.dbforge.common.testing.containers;

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
        DbforgePostgresContainer container = DbforgePostgresContainer.defaultInstance();
        assertThat(container.getDockerImageName()).isEqualTo("postgres:16-alpine");
        assertThat(container.getDatabaseName()).isEqualTo("dbforge");
    }

    @Test
    void mongoPinsImage() {
        DbforgeMongoContainer container = DbforgeMongoContainer.defaultInstance();
        assertThat(container.getDockerImageName()).isEqualTo("mongo:7.0");
    }

    @Test
    void redisExposesTheStandardPort() {
        DbforgeRedisContainer container = DbforgeRedisContainer.defaultInstance();
        assertThat(container.getExposedPorts()).containsExactly(6379);
    }

    @Test
    void redpandaPinsImage() {
        DbforgeRedpandaContainer container = DbforgeRedpandaContainer.defaultInstance();
        assertThat(container.getDockerImageName()).isEqualTo("redpandadata/redpanda:v24.2.7");
    }
}
