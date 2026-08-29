package com.dbforge.services.catalog.repository;

import com.dbforge.common.core.pagination.CursorPage;
import com.dbforge.common.core.pagination.Cursors;
import com.dbforge.common.core.pagination.PageRequest;
import com.dbforge.services.catalog.domain.Problem;
import com.dbforge.services.catalog.domain.ProblemFilter;
import com.dbforge.services.catalog.domain.TagCount;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.dbforge.services.catalog.repository.ProblemDocumentMapper.*;

/**
 * Cursor is the opaque encoding (via {@link Cursors}) of the raw sort key
 * {@code "<createdAtEpochMillis>|<id>"} - the same composite key the sort
 * itself uses ({@code createdAt} ascending, {@code _id} ascending as the
 * tiebreaker for problems created in the same millisecond), so a page
 * boundary is always unambiguous even though {@code createdAt} alone is
 * not unique.
 */
@Repository
public class MongoProblemRepository implements ProblemRepository {

    private final MongoCollection<Document> collection;

    public MongoProblemRepository(MongoCollection<Document> problemsCollection) {
        this.collection = problemsCollection;
    }

    @Override
    public void insert(Problem problem) {
        collection.insertOne(toDocument(problem));
    }

    @Override
    public void replace(Problem problem) {
        collection.replaceOne(Filters.eq(ID, problem.id().value()), toDocument(problem));
    }

    @Override
    public Optional<Problem> findBySlug(String slug) {
        Document document = collection.find(Filters.eq(SLUG, slug)).first();
        return Optional.ofNullable(document).map(ProblemDocumentMapper::fromDocument);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return collection.countDocuments(Filters.eq(SLUG, slug)) > 0;
    }

    @Override
    public CursorPage<Problem> findPage(ProblemFilter filter, PageRequest pageRequest) {
        List<Bson> clauses = new ArrayList<>();
        filter.tag().ifPresent(tag -> clauses.add(Filters.eq(TAGS, tag)));
        filter.difficulty().ifPresent(difficulty -> clauses.add(Filters.eq(DIFFICULTY, difficulty.name())));
        filter.engine().ifPresent(engine -> clauses.add(Filters.eq(ALLOWED_ENGINES, engine.name())));
        filter.titleSearch().ifPresent(search ->
                clauses.add(Filters.regex(TITLE, java.util.regex.Pattern.quote(search), "i")));
        if (filter.publishedOnly()) {
            clauses.add(Filters.eq(PUBLISHED, true));
        }

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
        List<Problem> problems = pageDocuments.stream().map(ProblemDocumentMapper::fromDocument).toList();

        if (!hasMore || problems.isEmpty()) {
            return CursorPage.lastPage(problems);
        }
        Problem last = problems.get(problems.size() - 1);
        String nextCursor = Cursors.encode(last.createdAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(problems, nextCursor);
    }

    @Override
    public List<TagCount> listTagCounts(boolean publishedOnly) {
        List<Bson> pipeline = new ArrayList<>();
        if (publishedOnly) {
            pipeline.add(Aggregates.match(Filters.eq(PUBLISHED, true)));
        }
        pipeline.add(Aggregates.unwind("$" + TAGS));
        pipeline.add(Aggregates.group("$" + TAGS, com.mongodb.client.model.Accumulators.sum("count", 1)));
        pipeline.add(Aggregates.sort(Sorts.descending("count")));

        List<TagCount> result = new ArrayList<>();
        for (Document document : collection.aggregate(pipeline)) {
            // $sum's result may decode as Integer or Long depending on magnitude -
            // go through Number rather than assuming one BSON numeric type.
            Number count = (Number) document.get("count");
            result.add(new TagCount(document.getString(ID), count.longValue()));
        }
        return result;
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
