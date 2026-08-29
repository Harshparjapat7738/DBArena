package com.DBArena.services.user.web.dto;

import com.DBArena.services.user.domain.Bookmark;

public record BookmarkResponse(String problemSlug, long bookmarkedAtEpochMillis) {

    public static BookmarkResponse from(Bookmark bookmark) {
        return new BookmarkResponse(bookmark.problemSlug(), bookmark.bookmarkedAtEpochMillis());
    }
}
