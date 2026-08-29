package com.DBArena.services.catalog.repository.topic;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.topic.Topic;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.DBArena.services.catalog.repository.topic.TopicDocumentMapper.*;

@Repository
public class MongoTopicRepository implements TopicRepository {

    private final MongoCollection<Document> collection;

    public MongoTopicRepository(MongoCollection<Document> topicsCollection) {
        this.collection = topicsCollection;
    }

    @Override
    public void insert(Topic topic) {
        collection.insertOne(toDocument(topic));
    }

    @Override
    public Optional<Topic> findBySlug(String slug) {
        Document document = collection.find(Filters.eq(SLUG, slug)).first();
        return Optional.ofNullable(document).map(TopicDocumentMapper::fromDocument);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return collection.countDocuments(Filters.eq(SLUG, slug)) > 0;
    }

    @Override
    public CursorPage<Topic> findPage(PageRequest pageRequest) {
        Bson query = pageRequest.cursor().map(this::afterCursor).orElse(Filters.empty());
        int limit = pageRequest.limit();

        List<Document> raw = new ArrayList<>();
        collection.find(query)
                .sort(Sorts.orderBy(Sorts.ascending(CREATED_AT), Sorts.ascending(ID)))
                .limit(limit + 1)
                .into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<Topic> topics = pageDocuments.stream().map(TopicDocumentMapper::fromDocument).toList();

        if (!hasMore || topics.isEmpty()) {
            return CursorPage.lastPage(topics);
        }
        Topic last = topics.get(topics.size() - 1);
        String nextCursor = Cursors.encode(last.createdAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(topics, nextCursor);
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
