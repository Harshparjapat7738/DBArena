package com.dbforge.services.catalog.repository;

import com.dbforge.common.core.id.TypedId;
import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.engine.spi.EngineType;
import com.dbforge.services.catalog.domain.Problem;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDocumentMapperTest {

    @Test
    void roundTripsEveryField() {
        Problem problem = new Problem(
                TypedId.of("01J000PROBLEM"),
                "two-sum",
                "Two Sum",
                "Given an array of integers...",
                Difficulty.EASY,
                Set.of("arrays", "hash-map"),
                Set.of(EngineType.POSTGRES, EngineType.MONGODB),
                "two-sum-dataset",
                true,
                1_700_000_000_000L,
                1_700_000_100_000L);

        Document document = ProblemDocumentMapper.toDocument(problem);
        Problem roundTripped = ProblemDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(problem);
    }

    @Test
    void timestampsAreStoredAsPlainLongsNotBsonDates() {
        Problem problem = new Problem(
                TypedId.of("01J000PROBLEM2"),
                "join-basics",
                "Join Basics",
                "Write a join...",
                Difficulty.MEDIUM,
                Set.of(),
                Set.of(EngineType.POSTGRES),
                "join-dataset",
                false,
                42L,
                43L);

        Document document = ProblemDocumentMapper.toDocument(problem);

        assertThat(document.get("createdAt")).isInstanceOf(Long.class);
        assertThat(document.get("updatedAt")).isInstanceOf(Long.class);
    }
}
