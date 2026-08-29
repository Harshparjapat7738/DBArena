package com.DBArena.common.testing.containers;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/** Pinned, reusable MongoDB fixture matching the version {@code make up} runs locally. */
public final class DBArenaMongoContainer extends MongoDBContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("mongo:7.0");

    public DBArenaMongoContainer() {
        super(IMAGE);
        withReuse(true);
    }

    public static DBArenaMongoContainer defaultInstance() {
        return new DBArenaMongoContainer();
    }
}
