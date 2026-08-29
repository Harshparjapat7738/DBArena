package com.DBArena.services.submission.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.submission.domain.Submission;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.DBArena.services.submission.repository.SubmissionDocumentMapper.*;

/**
 * Descending on {@code submittedAt} with {@code _id} as tiebreaker (both
 * ULIDs, so lexicographic {@code _id} order agrees with insertion order even
 * within the same millisecond) - "most recent first" is what every consumer
 * of this collection wants (a user's submission history, a problem's attempt
 * list), unlike catalog-service's oldest-first browse order.
 */
@Repository
public class MongoSubmissionRepository implements SubmissionRepository {

    private final MongoCollection<Document> collection;

    public MongoSubmissionRepository(MongoCollection<Document> submissionsCollection) {
        this.collection = submissionsCollection;
    }

    @Override
    public void insert(Submission submission) {
        collection.insertOne(toDocument(submission));
    }

    @Override
    public void replace(Submission submission) {
        collection.replaceOne(Filters.eq(ID, submission.id().value()), toDocument(submission));
    }

    @Override
    public Optional<Submission> findById(TypedId<Submission> id) {
        Document document = collection.find(Filters.eq(ID, id.value())).first();
        return Optional.ofNullable(document).map(SubmissionDocumentMapper::fromDocument);
    }

    @Override
    public CursorPage<Submission> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest) {
        return findPage(Filters.eq(USER_ID, userId.value()), pageRequest);
    }

    @Override
    public CursorPage<Submission> findPageByUserIdAndProblemSlug(
            TypedId<AuthenticatedUser> userId, String problemSlug, PageRequest pageRequest) {
        return findPage(Filters.and(Filters.eq(USER_ID, userId.value()), Filters.eq(PROBLEM_SLUG, problemSlug)), pageRequest);
    }

    private CursorPage<Submission> findPage(Bson baseFilter, PageRequest pageRequest) {
        List<Bson> clauses = new ArrayList<>();
        clauses.add(baseFilter);
        pageRequest.cursor().ifPresent(cursor -> clauses.add(beforeCursor(cursor)));
        Bson query = Filters.and(clauses);
        int limit = pageRequest.limit();

        List<Document> raw = new ArrayList<>();
        collection.find(query)
                .sort(Sorts.orderBy(Sorts.descending(SUBMITTED_AT), Sorts.descending(ID)))
                .limit(limit + 1)
                .into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<Submission> submissions = pageDocuments.stream().map(SubmissionDocumentMapper::fromDocument).toList();

        if (!hasMore || submissions.isEmpty()) {
            return CursorPage.lastPage(submissions);
        }
        Submission last = submissions.get(submissions.size() - 1);
        String nextCursor = Cursors.encode(last.submittedAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(submissions, nextCursor);
    }

    @Override
    public Map<String, String> findStatusesByUserId(TypedId<AuthenticatedUser> userId) {
        List<Bson> pipeline = List.of(
                Aggregates.match(Filters.eq(USER_ID, userId.value())),
                Aggregates.group("$" + PROBLEM_SLUG, Accumulators.max("anyAccepted",
                        new Document("$cond", List.of(new Document("$eq", List.of("$" + STATUS, "ACCEPTED")), 1, 0)))));

        Map<String, String> statuses = new LinkedHashMap<>();
        for (Document document : collection.aggregate(pipeline)) {
            Number anyAccepted = (Number) document.get("anyAccepted");
            statuses.put(document.getString(ID), anyAccepted != null && anyAccepted.intValue() == 1 ? "SOLVED" : "ATTEMPTED");
        }
        return statuses;
    }

    private static Bson beforeCursor(String cursor) {
        String raw = Cursors.decode(cursor);
        int separator = raw.indexOf('|');
        if (separator < 0) {
            throw new Cursors.InvalidCursorException(cursor, null);
        }
        long submittedAt = Long.parseLong(raw.substring(0, separator));
        String id = raw.substring(separator + 1);
        return Filters.or(
                Filters.lt(SUBMITTED_AT, submittedAt),
                Filters.and(Filters.eq(SUBMITTED_AT, submittedAt), Filters.lt(ID, id)));
    }
}
