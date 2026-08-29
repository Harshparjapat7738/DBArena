package com.DBArena.services.gamification.repository.activity;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.gamification.domain.activity.ActivityItem;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityItemDocumentMapperTest {

    @Test
    void roundTripsWithARefSlug() {
        ActivityItem item = new ActivityItem(
                TypedId.of("01J000ACTIVITY"), TypedId.of("01J000USER"), "SUBMISSION_ACCEPTED",
                "Solved Two Sum", Optional.of("two-sum"), 1_700_000_000_000L);

        Document document = ActivityItemDocumentMapper.toDocument(item);
        ActivityItem roundTripped = ActivityItemDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(item);
    }

    @Test
    void roundTripsWithoutARefSlug() {
        ActivityItem item = new ActivityItem(
                TypedId.of("01J000ACTIVITY2"), TypedId.of("01J000USER"), "BADGE_EARNED",
                "Earned First Blood", Optional.empty(), 1_700_000_000_000L);

        Document document = ActivityItemDocumentMapper.toDocument(item);
        ActivityItem roundTripped = ActivityItemDocumentMapper.fromDocument(document);

        assertThat(roundTripped.refSlug()).isEmpty();
        assertThat(roundTripped).isEqualTo(item);
    }
}
