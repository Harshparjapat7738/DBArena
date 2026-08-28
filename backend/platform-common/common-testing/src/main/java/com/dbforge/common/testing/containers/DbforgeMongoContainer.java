package com.dbforge.common.testing.containers;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/** Pinned, reusable MongoDB fixture matching the version {@code make up} runs locally. */
public final class DbforgeMongoContainer extends MongoDBContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("mongo:7.0");

    public DbforgeMongoContainer() {
        super(IMAGE);
        withReuse(true);
    }

    public static DbforgeMongoContainer defaultInstance() {
        return new DbforgeMongoContainer();
    }
}
