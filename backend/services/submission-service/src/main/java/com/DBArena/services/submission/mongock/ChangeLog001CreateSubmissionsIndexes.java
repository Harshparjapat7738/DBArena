package com.DBArena.services.submission.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * No unique index here - unlike every other B02 collection, a user may
 * submit the same problem arbitrarily many times, so uniqueness is never
 * the right constraint. Both indexes are compound and lead with
 * {@code userId} since every real query is scoped to one user first.
 */
@ChangeUnit(id = "001-create-submissions-indexes", order = "001", author = "submission-service")
public class ChangeLog001CreateSubmissionsIndexes {

    private static final String COLLECTION = "submissions";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        var collection = mongoDatabase.getCollection(COLLECTION);
        collection.createIndex(Indexes.descending("userId", "submittedAt", "_id"));
        collection.createIndex(Indexes.descending("userId", "problemSlug", "submittedAt", "_id"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).drop();
    }
}
