package com.DBArena.services.user.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

@ChangeUnit(id = "001-create-bookmarks-indexes", order = "001", author = "user-service")
public class ChangeLog001CreateBookmarksIndexes {

    private static final String COLLECTION = "bookmarks";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        var collection = mongoDatabase.getCollection(COLLECTION);
        // One bookmark per (user, problem) - toggling is idempotent at the repository layer,
        // this index is the backstop against a race producing duplicates.
        collection.createIndex(Indexes.ascending("userId", "problemSlug"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("userId", "bookmarkedAt", "_id"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).drop();
    }
}
