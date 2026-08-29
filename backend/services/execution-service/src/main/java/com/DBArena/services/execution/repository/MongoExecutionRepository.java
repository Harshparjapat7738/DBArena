package com.DBArena.services.execution.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.execution.domain.Execution;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.DBArena.services.execution.repository.ExecutionDocumentMapper.*;

/** Most-recent-first per user - same convention as submission-service's own high-write-volume collection. */
@Repository
public class MongoExecutionRepository implements ExecutionRepository {

    private final MongoCollection<Document> collection;

    public MongoExecutionRepository(MongoCollection<Document> executionsCollection) {
        this.collection = executionsCollection;
    }

    @Override
    public void insert(Execution execution) {
        collection.insertOne(toDocument(execution));
    }

    @Override
    public void replace(Execution execution) {
        collection.replaceOne(Filters.eq(ID, execution.id().value()), toDocument(execution));
    }

    @Override
    public Optional<Execution> findById(TypedId<Execution> id) {
        Document document = collection.find(Filters.eq(ID, id.value())).first();
        return Optional.ofNullable(document).map(ExecutionDocumentMapper::fromDocument);
    }

    @Override
    public CursorPage<Execution> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest) {
        List<Bson> clauses = new ArrayList<>();
        clauses.add(Filters.eq(USER_ID, userId.value()));
        pageRequest.cursor().ifPresent(cursor -> clauses.add(beforeCursor(cursor)));
        Bson query = Filters.and(clauses);
        int limit = pageRequest.limit();

        List<Document> raw = new ArrayList<>();
        collection.find(query)
                .sort(Sorts.orderBy(Sorts.descending(REQUESTED_AT), Sorts.descending(ID)))
                .limit(limit + 1)
                .into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<Execution> executions = pageDocuments.stream().map(ExecutionDocumentMapper::fromDocument).toList();

        if (!hasMore || executions.isEmpty()) {
            return CursorPage.lastPage(executions);
        }
        Execution last = executions.get(executions.size() - 1);
        String nextCursor = Cursors.encode(last.requestedAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(executions, nextCursor);
    }

    private static Bson beforeCursor(String cursor) {
        String raw = Cursors.decode(cursor);
        int separator = raw.indexOf('|');
        if (separator < 0) {
            throw new Cursors.InvalidCursorException(cursor, null);
        }
        long requestedAt = Long.parseLong(raw.substring(0, separator));
        String id = raw.substring(separator + 1);
        return Filters.or(
                Filters.lt(REQUESTED_AT, requestedAt),
                Filters.and(Filters.eq(REQUESTED_AT, requestedAt), Filters.lt(ID, id)));
    }
}
