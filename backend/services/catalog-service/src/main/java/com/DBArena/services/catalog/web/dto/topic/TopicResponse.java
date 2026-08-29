package com.DBArena.services.catalog.web.dto.topic;

import com.DBArena.services.catalog.domain.topic.Topic;

public record TopicResponse(String slug, String name, String description) {

    public static TopicResponse from(Topic topic) {
        return new TopicResponse(topic.slug(), topic.name(), topic.description());
    }
}
