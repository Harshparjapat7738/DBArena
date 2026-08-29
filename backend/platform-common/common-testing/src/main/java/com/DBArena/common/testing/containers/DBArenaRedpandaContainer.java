package com.dbforge.common.testing.containers;

import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pinned, reusable Redpanda fixture (Kafka-API-compatible), matching what
 * {@code make up} runs locally in place of a full Kafka + ZooKeeper stack.
 */
public final class DbforgeRedpandaContainer extends RedpandaContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("redpandadata/redpanda:v24.2.7");

    public DbforgeRedpandaContainer() {
        super(IMAGE);
        withReuse(true);
    }

    public static DbforgeRedpandaContainer defaultInstance() {
        return new DbforgeRedpandaContainer();
    }
}
