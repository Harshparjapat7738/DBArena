package com.DBArena.services.user.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.Cursors;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.user.domain.Bookmark;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.DBArena.services.user.repository.BookmarkDocumentMapper.*;

@Repository
public class MongoBookmarkRepository implements BookmarkRepository {

    private final MongoCollection<Document> collection;

    public MongoBookmarkRepository(MongoCollection<Document> bookmarksCollection) {
        this.collection = bookmarksCollection;
    }

    @Override
    public void insert(Bookmark bookmark) {
        collection.insertOne(toDocument(bookmark));
    }

    @Override
    public void deleteByUserIdAndProblemSlug(TypedId<AuthenticatedUser> userId, String problemSlug) {
        collection.deleteOne(Filters.and(Filters.eq(USER_ID, userId.value()), Filters.eq(PROBLEM_SLUG, problemSlug)));
    }

    @Override
    public boolean existsByUserIdAndProblemSlug(TypedId<AuthenticatedUser> userId, String problemSlug) {
        return collection.countDocuments(
                Filters.and(Filters.eq(USER_ID, userId.value()), Filters.eq(PROBLEM_SLUG, problemSlug))) > 0;
    }

    @Override
    public CursorPage<Bookmark> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest) {
        List<Bson> clauses = new ArrayList<>();
        clauses.add(Filters.eq(USER_ID, userId.value()));
        pageRequest.cursor().ifPresent(cursor -> clauses.add(afterCursor(cursor)));
        Bson query = Filters.and(clauses);
        int limit = pageRequest.limit();

        List<Document> raw = new ArrayList<>();
        collection.find(query)
                .sort(Sorts.orderBy(Sorts.ascending(BOOKMARKED_AT), Sorts.ascending(ID)))
                .limit(limit + 1)
                .into(raw);

        boolean hasMore = raw.size() > limit;
        List<Document> pageDocuments = hasMore ? raw.subList(0, limit) : raw;
        List<Bookmark> bookmarks = pageDocuments.stream().map(BookmarkDocumentMapper::fromDocument).toList();

        if (!hasMore || bookmarks.isEmpty()) {
            return CursorPage.lastPage(bookmarks);
        }
        Bookmark last = bookmarks.get(bookmarks.size() - 1);
        String nextCursor = Cursors.encode(last.bookmarkedAtEpochMillis() + "|" + last.id().value());
        return CursorPage.of(bookmarks, nextCursor);
    }

    @Override
    public List<String> findAllProblemSlugsByUserId(TypedId<AuthenticatedUser> userId) {
        List<String> slugs = new ArrayList<>();
        for (Document document : collection.find(Filters.eq(USER_ID, userId.value()))) {
            slugs.add(document.getString(PROBLEM_SLUG));
        }
        return slugs;
    }

    private static Bson afterCursor(String cursor) {
        String raw = Cursors.decode(cursor);
        int separator = raw.indexOf('|');
        if (separator < 0) {
            throw new Cursors.InvalidCursorException(cursor, null);
        }
        long bookmarkedAt = Long.parseLong(raw.substring(0, separator));
        String id = raw.substring(separator + 1);
        return Filters.or(
                Filters.gt(BOOKMARKED_AT, bookmarkedAt),
                Filters.and(Filters.eq(BOOKMARKED_AT, bookmarkedAt), Filters.gt(ID, id)));
    }
}
