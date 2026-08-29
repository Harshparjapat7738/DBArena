package com.DBArena.services.catalog.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * B03: {@code datasetSlug} became a real query predicate (the {@code dataset}
 * filter on {@code /api/v1/problems}, the related-problems candidate query,
 * and {@code Dataset.problemCount}) rather than just stored, opaque data -
 * ChangeLog001 never indexed it because nothing queried by it yet.
 */
@ChangeUnit(id = "005-create-problems-dataset-slug-index", order = "005", author = "catalog-service")
public class ChangeLog005CreateProblemsDatasetSlugIndex {

    private static final String COLLECTION = "problems";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).createIndex(Indexes.ascending("datasetSlug"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(COLLECTION).dropIndex(Indexes.ascending("datasetSlug"));
    }
}
