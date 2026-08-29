package com.DBArena.services.execution.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

@ChangeUnit(id = "001-create-executions-indexes", order = "001", author = "execution-service")
public class ChangeLog001CreateExecutionsIndexes {

    private static final String COLLECTION = "executions";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION)
                .createIndex(Indexes.descending("userId", "requestedAt", "_id"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).drop();
    }
}
