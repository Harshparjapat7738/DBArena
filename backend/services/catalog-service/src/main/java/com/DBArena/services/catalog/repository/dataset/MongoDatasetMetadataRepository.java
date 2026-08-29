package com.DBArena.services.catalog.repository.dataset;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.dataset.DatasetFilter;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.DBArena.services.catalog.repository.dataset.DatasetMetadataDocumentMapper.*;

@Repository
public class MongoDatasetMetadataRepository implements DatasetMetadataRepository {

    private final MongoCollection<Document> collection;

    public MongoDatasetMetadataRepository(MongoCollection<Document> datasetMetadataCollection) {
        this.collection = datasetMetadataCollection;
    }

    @Override
    public void insert(DatasetMetadata dataset) {
        collection.insertOne(toDocument(dataset));
    }

    @Override
    public void replace(DatasetMetadata dataset) {
        collection.replaceOne(Filters.eq(ID, dataset.id().value()), toDocument(dataset));
    }

    @Override
    public Optional<DatasetMetadata> findBySlug(String slug) {
        Document document = collection.find(Filters.eq(SLUG, slug)).first();
        return Optional.ofNullable(document).map(DatasetMetadataDocumentMapper::fromDocument);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return collection.countDocuments(Filters.eq(SLUG, slug)) > 0;
    }

    @Override
    public CursorPage<DatasetMetadata> findPage(DatasetFilter filter, PageRequest pageRequest) {
        List<Bson> clauses = new ArrayList<>();
        filter.category().ifPresent(c -> clauses.add(Filters.eq(CATEGORY, c)));
        filter.engine().ifPresent(e -> clauses.add(Filters.eq(ENGINES, e.name())));
        filter.nameSearch().ifPresent(search ->
                clauses.add(Filters.regex(NAME, java.util.regex.Pattern.quote(search), "i")));
        pageRequest.cursor().ifPresent(cursor -> clauses.add(afterCursor(cursor)));
        Bson query = clauses.isEmpty() ? Filters.empty() : Filters.and(clauses);
        int limit = pageRequest.limit();

        List<Document> raw = new ArrayList<>();
        collection.find(query)
                .sort(Sorts.orderBy(Sorts.ascending(CREATED_AT), Sorts.ascending(ID)))
                .limit(limit + 1)
                .into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<DatasetMetadata> datasets = pageDocuments.stream().map(DatasetMetadataDocumentMapper::fromDocument).toList();

        if (!hasMore || datasets.isEmpty()) {
            return CursorPage.lastPage(datasets);
        }
        DatasetMetadata last = datasets.get(datasets.size() - 1);
        String nextCursor = Cursors.encode(last.createdAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(datasets, nextCursor);
    }

    private static Bson afterCursor(String cursor) {
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
