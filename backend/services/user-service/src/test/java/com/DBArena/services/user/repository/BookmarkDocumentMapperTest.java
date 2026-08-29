package com.DBArena.services.user.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.user.domain.Bookmark;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkDocumentMapperTest {

    @Test
    void roundTripsEveryField() {
        Bookmark bookmark = new Bookmark(
                TypedId.of("01J000BOOKMARK"), TypedId.of("01J000USER"), "two-sum", 1_700_000_000_000L);

        Document document = BookmarkDocumentMapper.toDocument(bookmark);
        Bookmark roundTripped = BookmarkDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(bookmark);
    }
}
