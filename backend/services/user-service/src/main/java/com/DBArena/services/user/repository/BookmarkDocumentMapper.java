package com.DBArena.services.user.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.user.domain.Bookmark;
import org.bson.Document;

public final class BookmarkDocumentMapper {

    static final String ID = "_id";
    static final String USER_ID = "userId";
    static final String PROBLEM_SLUG = "problemSlug";
    static final String BOOKMARKED_AT = "bookmarkedAt";

    private BookmarkDocumentMapper() {
    }

    public static Document toDocument(Bookmark bookmark) {
        return new Document()
                .append(ID, bookmark.id().value())
                .append(USER_ID, bookmark.userId().value())
                .append(PROBLEM_SLUG, bookmark.problemSlug())
                .append(BOOKMARKED_AT, bookmark.bookmarkedAtEpochMillis());
    }

    public static Bookmark fromDocument(Document document) {
        return new Bookmark(
                TypedId.of(document.getString(ID)),
                TypedId.of(document.getString(USER_ID)),
                document.getString(PROBLEM_SLUG),
                document.getLong(BOOKMARKED_AT));
    }
}
