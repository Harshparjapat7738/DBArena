package com.DBArena.services.catalog.repository.learning;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.learning.LearningPath;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.DBArena.services.catalog.repository.learning.LearningPathDocumentMapper.*;

@Repository
public class MongoLearningPathRepository implements LearningPathRepository {

    private final MongoCollection<Document> collection;

    public MongoLearningPathRepository(MongoCollection<Document> learningPathsCollection) {
        this.collection = learningPathsCollection;
    }

    @Override
    public void insert(LearningPath path) {
        collection.insertOne(toDocument(path));
    }

    @Override
    public void replace(LearningPath path) {
        collection.replaceOne(Filters.eq(ID, path.id().value()), toDocument(path));
    }

    @Override
    public Optional<LearningPath> findBySlug(String slug) {
        Document document = collection.find(Filters.eq(SLUG, slug)).first();
        return Optional.ofNullable(document).map(LearningPathDocumentMapper::fromDocument);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return collection.countDocuments(Filters.eq(SLUG, slug)) > 0;
    }

    @Override
    public CursorPage<LearningPath> findPage(PageRequest pageRequest) {
        Bson query = pageRequest.cursor().map(this::afterCursor).orElse(Filters.empty());
        int limit = pageRequest.limit();

        List<Document> raw = new ArrayList<>();
        collection.find(query)
                .sort(Sorts.orderBy(Sorts.ascending(CREATED_AT), Sorts.ascending(ID)))
                .limit(limit + 1)
                .into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<LearningPath> paths = pageDocuments.stream().map(LearningPathDocumentMapper::fromDocument).toList();

        if (!hasMore || paths.isEmpty()) {
            return CursorPage.lastPage(paths);
        }
        LearningPath last = paths.get(paths.size() - 1);
        String nextCursor = Cursors.encode(last.createdAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(paths, nextCursor);
    }

    private Bson afterCursor(String cursor) {
        String raw = Cursors.decode(cursor);
        int separator = raw.indexOf('|');
        if (separator < 0) {
            throw new Cursors.InvalidCursorException(cursor, null);
        }
        long createdAt = Long.parseLong(raw.substring(0, separator));
        String id = raw.substring(separator + 1);
        return Filters.or(
                Filters.gt(CREATED_AT, createdAt),
                Filters.and(Filters.eq(CREATED_AT, createdAt), Filters.gt(ID, id)));
    }
}
