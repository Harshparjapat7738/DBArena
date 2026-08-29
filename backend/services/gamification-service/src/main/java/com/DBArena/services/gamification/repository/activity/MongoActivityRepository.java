package com.DBArena.services.gamification.repository.activity;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.activity.ActivityItem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.DBArena.services.gamification.repository.activity.ActivityItemDocumentMapper.*;

/**
 * Most-recent-first, per user - descending sort on {@code occurredAt} with
 * {@code _id} as a tiebreaker, mirroring catalog-service's cursor convention
 * but reversed (feeds read newest-first, catalogs read oldest-first).
 */
@Repository
public class MongoActivityRepository implements ActivityRepository {

    private final MongoCollection<Document> collection;

    public MongoActivityRepository(MongoCollection<Document> activityCollection) {
        this.collection = activityCollection;
    }

    @Override
    public void insert(ActivityItem item) {
        collection.insertOne(toDocument(item));
    }

    @Override
    public CursorPage<ActivityItem> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest) {
        List<Bson> clauses = new ArrayList<>();
        clauses.add(Filters.eq(USER_ID, userId.value()));
        pageRequest.cursor().ifPresent(cursor -> clauses.add(beforeCursor(cursor)));
        Bson query = Filters.and(clauses);
        int limit = pageRequest.limit();

        List<Document> raw = new ArrayList<>();
        collection.find(query)
                .sort(Sorts.orderBy(Sorts.descending(OCCURRED_AT), Sorts.descending(ID)))
                .limit(limit + 1)
                .into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<ActivityItem> items = pageDocuments.stream().map(ActivityItemDocumentMapper::fromDocument).toList();

        if (!hasMore || items.isEmpty()) {
            return CursorPage.lastPage(items);
        }
        ActivityItem last = items.get(items.size() - 1);
        String nextCursor = Cursors.encode(last.occurredAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(items, nextCursor);
    }

    private static Bson beforeCursor(String cursor) {
        String raw = Cursors.decode(cursor);
        int separator = raw.indexOf('|');
        if (separator < 0) {
            throw new Cursors.InvalidCursorException(cursor, null);
        }
        long occurredAt = Long.parseLong(raw.substring(0, separator));
        String id = raw.substring(separator + 1);
        return Filters.or(
                Filters.lt(OCCURRED_AT, occurredAt),
                Filters.and(Filters.eq(OCCURRED_AT, occurredAt), Filters.lt(ID, id)));
    }
}
