package com.DBArena.services.catalog.repository;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.Problem;
import com.DBArena.services.catalog.domain.ProblemFilter;
import com.DBArena.services.catalog.domain.ProblemSort;
import com.DBArena.services.catalog.domain.TagCount;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.DBArena.services.catalog.repository.ProblemDocumentMapper.*;

/**
 * Cursor is the opaque encoding (via {@link Cursors}) of the raw sort key -
 * shape depends on {@link ProblemSort}: {@code "<createdAt>|<id>"} for
 * {@link ProblemSort#OLDEST_FIRST}/{@link ProblemSort#NEWEST_FIRST}, and
 * {@code "<difficultyRank>|<createdAt>|<id>"} for
 * {@link ProblemSort#DIFFICULTY_THEN_NEWEST} (computed via an aggregation
 * {@code $addFields}, never stored - Difficulty's declared enum order
 * (EASY, MEDIUM, HARD) already *is* the rank, so nothing needs migrating if
 * that order ever changes, just this one array literal).
 */
@Repository
public class MongoProblemRepository implements ProblemRepository {

    private static final String RANK_FIELD = "b03Rank";
    private static final List<String> DIFFICULTY_RANK_ORDER = List.of("EASY", "MEDIUM", "HARD");

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
    public CursorPage<Problem> findPage(ProblemFilter filter, PageRequest pageRequest, ProblemSort sort) {
        Bson baseQuery = baseQuery(filter);
        int limit = pageRequest.limit();

        return switch (sort) {
            case OLDEST_FIRST -> findPageByCreatedAt(baseQuery, pageRequest, limit, true);
            case NEWEST_FIRST -> findPageByCreatedAt(baseQuery, pageRequest, limit, false);
            case DIFFICULTY_THEN_NEWEST -> findPageByDifficulty(baseQuery, pageRequest, limit);
        };
    }

    private Bson baseQuery(ProblemFilter filter) {
        List<Bson> clauses = new ArrayList<>();
        filter.tag().ifPresent(tag -> clauses.add(Filters.eq(TAGS, tag)));
        filter.difficulty().ifPresent(difficulty -> clauses.add(Filters.eq(DIFFICULTY, difficulty.name())));
        filter.engine().ifPresent(engine -> clauses.add(Filters.eq(ALLOWED_ENGINES, engine.name())));
        filter.titleSearch().ifPresent(search ->
                clauses.add(Filters.regex(TITLE, java.util.regex.Pattern.quote(search), "i")));
        filter.datasetSlug().ifPresent(datasetSlug -> clauses.add(Filters.eq(DATASET_SLUG, datasetSlug)));
        filter.slugIn().ifPresent(slugs -> clauses.add(Filters.in(SLUG, slugs)));
        filter.slugNotIn().ifPresent(slugs -> clauses.add(Filters.nin(SLUG, slugs)));
        if (filter.publishedOnly()) {
            clauses.add(Filters.eq(PUBLISHED, true));
        }
        return clauses.isEmpty() ? Filters.empty() : Filters.and(clauses);
    }

    private CursorPage<Problem> findPageByCreatedAt(Bson baseQuery, PageRequest pageRequest, int limit, boolean ascending) {
        List<Bson> clauses = new ArrayList<>();
        clauses.add(baseQuery);
        pageRequest.cursor().ifPresent(cursor -> clauses.add(createdAtCursorClause(cursor, ascending)));
        Bson query = Filters.and(clauses);

        List<Document> raw = new ArrayList<>();
        Bson sortSpec = ascending
                ? Sorts.orderBy(Sorts.ascending(CREATED_AT), Sorts.ascending(ID))
                : Sorts.orderBy(Sorts.descending(CREATED_AT), Sorts.descending(ID));
        collection.find(query).sort(sortSpec).limit(limit + 1).into(raw);

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

    private static Bson createdAtCursorClause(String cursor, boolean ascending) {
        String raw = Cursors.decode(cursor);
        int separator = raw.indexOf('|');
        if (separator < 0) {
            throw new Cursors.InvalidCursorException(cursor, null);
        }
        long createdAt = Long.parseLong(raw.substring(0, separator));
        String id = raw.substring(separator + 1);
        return ascending
                ? Filters.or(Filters.gt(CREATED_AT, createdAt), Filters.and(Filters.eq(CREATED_AT, createdAt), Filters.gt(ID, id)))
                : Filters.or(Filters.lt(CREATED_AT, createdAt), Filters.and(Filters.eq(CREATED_AT, createdAt), Filters.lt(ID, id)));
    }

    private CursorPage<Problem> findPageByDifficulty(Bson baseQuery, PageRequest pageRequest, int limit) {
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.match(baseQuery));
        pipeline.add(Aggregates.addFields(new Field<>(RANK_FIELD,
                new Document("$indexOfArray", List.of(DIFFICULTY_RANK_ORDER, "$" + DIFFICULTY)))));
        pageRequest.cursor().ifPresent(cursor -> pipeline.add(Aggregates.match(difficultyCursorClause(cursor))));
        pipeline.add(Aggregates.sort(Sorts.orderBy(
                Sorts.ascending(RANK_FIELD), Sorts.descending(CREATED_AT), Sorts.descending(ID))));
        pipeline.add(Aggregates.limit(limit + 1));

        List<Document> raw = new ArrayList<>();
        collection.aggregate(pipeline).into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<Problem> problems = pageDocuments.stream().map(ProblemDocumentMapper::fromDocument).toList();

        if (!hasMore || pageDocuments.isEmpty()) {
            return CursorPage.lastPage(problems);
        }
        Document last = pageDocuments.get(pageDocuments.size() - 1);
        String nextCursor = Cursors.encode(
                last.getInteger(RANK_FIELD) + "|" + last.getLong(CREATED_AT) + "|" + last.getString(ID));
        return CursorPage.of(problems, nextCursor);
    }

    private static Bson difficultyCursorClause(String cursor) {
        String raw = Cursors.decode(cursor);
        String[] parts = raw.split("\\|", 3);
        if (parts.length != 3) {
            throw new Cursors.InvalidCursorException(cursor, null);
        }
        int rank = Integer.parseInt(parts[0]);
        long createdAt = Long.parseLong(parts[1]);
        String id = parts[2];
        return Filters.or(
                Filters.gt(RANK_FIELD, rank),
                Filters.and(Filters.eq(RANK_FIELD, rank), Filters.lt(CREATED_AT, createdAt)),
                Filters.and(Filters.eq(RANK_FIELD, rank), Filters.eq(CREATED_AT, createdAt), Filters.lt(ID, id)));
    }

    @Override
    public List<TagCount> listTagCounts(boolean publishedOnly) {
        List<Bson> pipeline = new ArrayList<>();
        if (publishedOnly) {
            pipeline.add(Aggregates.match(Filters.eq(PUBLISHED, true)));
        }
        pipeline.add(Aggregates.unwind("$" + TAGS));
        pipeline.add(Aggregates.group("$" + TAGS, Accumulators.sum("count", 1)));
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

    @Override
    public List<Problem> findRelatedCandidates(String datasetSlug, Set<String> tags, String excludeSlug, int candidateLimit) {
        List<Bson> orClauses = new ArrayList<>();
        if (datasetSlug != null && !datasetSlug.isBlank()) {
            orClauses.add(Filters.eq(DATASET_SLUG, datasetSlug));
        }
        if (!tags.isEmpty()) {
            orClauses.add(Filters.in(TAGS, tags));
        }
        if (orClauses.isEmpty()) {
            return List.of();
        }
        Bson query = Filters.and(
                Filters.eq(PUBLISHED, true),
                Filters.ne(SLUG, excludeSlug),
                Filters.or(orClauses));

        List<Problem> result = new ArrayList<>();
        for (Document document : collection.find(query).limit(candidateLimit)) {
            result.add(ProblemDocumentMapper.fromDocument(document));
        }
        return result;
    }

    @Override
    public long countPublishedByDatasetSlug(String datasetSlug) {
        return collection.countDocuments(Filters.and(Filters.eq(DATASET_SLUG, datasetSlug), Filters.eq(PUBLISHED, true)));
    }
}
