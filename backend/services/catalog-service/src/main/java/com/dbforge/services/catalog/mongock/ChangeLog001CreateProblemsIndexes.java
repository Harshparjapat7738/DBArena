package com.dbforge.services.catalog.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * Mongock is this platform's Mongo counterpart to Flyway (backend/CLAUDE.md
 * "Conventions"). This is the highest reconstructed-from-memory risk in
 * this milestone: the exact annotation package
 * ({@code io.mongock.api.annotations}), the {@code @ChangeUnit} attribute
 * names, and the Spring Boot integration property that points Mongock at
 * this package ({@code mongock.migration-scan-package}, set in
 * application.yml) were all written from memory, not verified against the
 * real dependency jars (same standing network limitation as every prior
 * milestone). Double check this whole package first if {@code mvn verify}
 * fails on catalog-service.
 */
@ChangeUnit(id = "001-create-problems-indexes", order = "001", author = "catalog-service")
public class ChangeLog001CreateProblemsIndexes {

    private static final String COLLECTION = "problems";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        var collection = mongoDatabase.getCollection(COLLECTION);
        collection.createIndex(Indexes.ascending("slug"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("createdAt", "_id"));
        collection.createIndex(Indexes.ascending("tags"));
        collection.createIndex(Indexes.ascending("difficulty"));
        collection.createIndex(Indexes.ascending("published"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).drop();
    }
}
