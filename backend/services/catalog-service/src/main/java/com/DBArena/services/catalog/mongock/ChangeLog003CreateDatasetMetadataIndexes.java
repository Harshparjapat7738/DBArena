package com.DBArena.services.catalog.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

@ChangeUnit(id = "003-create-dataset-metadata-indexes", order = "003", author = "catalog-service")
public class ChangeLog003CreateDatasetMetadataIndexes {

    private static final String COLLECTION = "dataset_metadata";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        var collection = mongoDatabase.getCollection(COLLECTION);
        collection.createIndex(Indexes.ascending("slug"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("createdAt", "_id"));
        collection.createIndex(Indexes.ascending("category"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).drop();
    }
}
