package com.DBArena.services.catalog.repository.learning;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.learning.LearningLevel;
import com.DBArena.services.catalog.domain.learning.LearningPath;
import com.DBArena.services.catalog.domain.learning.Lesson;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LearningPathDocumentMapperTest {

    @Test
    void roundTripsEveryFieldIncludingEmbeddedLessons() {
        LearningPath path = new LearningPath(
                TypedId.of("01J000PATH"),
                "sql-fundamentals",
                "SQL Fundamentals",
                "Start here",
                LearningLevel.BEGINNER,
                EngineType.POSTGRES,
                List.of(
                        new Lesson("select-basics", "SELECT Basics", "Filtering rows", 15, 1, Optional.of("two-sum")),
                        new Lesson("joins", "Joins", "Combining tables", 20, 2, Optional.empty())),
                2,
                1,
                1_700_000_000_000L,
                1_700_000_100_000L);

        Document document = LearningPathDocumentMapper.toDocument(path);
        LearningPath roundTripped = LearningPathDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(path);
    }
}
