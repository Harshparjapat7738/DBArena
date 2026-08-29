package com.DBArena.services.catalog.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

@ChangeUnit(id = "002-create-topics-indexes", order = "002", author = "catalog-service")
public class ChangeLog002CreateTopicsIndexes {

    private static final String COLLECTION = "topics";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        var collection = mongoDatabase.getCollection(COLLECTION);
        collection.createIndex(Indexes.ascending("slug"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("createdAt", "_id"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).drop();
    }
}
