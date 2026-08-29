package com.DBArena.services.catalog.repository.topic;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.catalog.domain.topic.Topic;
import org.bson.Document;

/** Pure, dependency-free mapper - same convention as {@code ProblemDocumentMapper}. */
public final class TopicDocumentMapper {

    static final String ID = "_id";
    static final String SLUG = "slug";
    static final String NAME = "name";
    static final String DESCRIPTION = "description";
    static final String CREATED_AT = "createdAt";

    private TopicDocumentMapper() {
    }

    public static Document toDocument(Topic topic) {
        return new Document()
                .append(ID, topic.id().value())
                .append(SLUG, topic.slug())
                .append(NAME, topic.name())
                .append(DESCRIPTION, topic.description())
                .append(CREATED_AT, topic.createdAtEpochMillis());
    }

    public static Topic fromDocument(Document document) {
        return new Topic(
                TypedId.of(document.getString(ID)),
                document.getString(SLUG),
                document.getString(NAME),
                document.getString(DESCRIPTION),
                document.getLong(CREATED_AT));
    }
}
