package com.DBArena.services.catalog.repository.topic;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.catalog.domain.topic.Topic;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopicDocumentMapperTest {

    @Test
    void roundTripsEveryField() {
        Topic topic = new Topic(TypedId.of("01J000TOPIC"), "arrays", "Arrays", "Array manipulation problems", 1_700_000_000_000L);

        Document document = TopicDocumentMapper.toDocument(topic);
        Topic roundTripped = TopicDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(topic);
    }
}
