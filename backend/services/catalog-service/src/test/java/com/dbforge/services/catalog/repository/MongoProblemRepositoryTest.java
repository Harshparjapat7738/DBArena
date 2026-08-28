package com.dbforge.services.catalog.repository;

import com.dbforge.common.core.id.TypedId;
import com.dbforge.common.core.pagination.CursorPage;
import com.dbforge.common.core.pagination.PageRequest;
import com.dbforge.common.testing.containers.DbforgeMongoContainer;
import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.engine.spi.EngineType;
import com.dbforge.services.catalog.domain.Problem;
import com.dbforge.services.catalog.domain.ProblemFilter;
import com.dbforge.services.catalog.domain.TagCount;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hard rule #3: never mock a database - real Mongo via Testcontainers.
 * Indexes are normally created by the Mongock changelog at app startup;
 * this test talks to the repository directly (no Spring context, no
 * Mongock run) so it deliberately does not depend on them existing -
 * every assertion here holds with or without indexes, since none of them
 * test uniqueness enforcement at the storage layer (that's covered at the
 * service layer by {@code existsBySlug} - see CatalogServiceTest).
 */
@Testcontainers
class MongoProblemRepositoryTest {

    @Container
    static final DbforgeMongoContainer MONGO = DbforgeMongoContainer.defaultInstance();

    private static MongoClient client;
    private MongoProblemRepository repository;

    @BeforeAll
    static void startClient() {
        client = MongoClients.create(MONGO.getReplicaSetUrl());
    }

    @AfterAll
    static void stopClient() {
        client.close();
    }

    @BeforeEach
    void freshCollection() {
        String dbName = "test_" + System.nanoTime();
        MongoCollection<Document> collection = client.getDatabase(dbName).getCollection("problems");
        repository = new MongoProblemRepository(collection);
    }

    private static Problem problem(String slug, Difficulty difficulty, Set<String> tags, long createdAt) {
        return new Problem(
                TypedId.of("id-" + slug),
                slug,
                "Title " + slug,
                "Statement for " + slug,
                difficulty,
                tags,
                Set.of(EngineType.POSTGRES),
                slug + "-dataset",
                true,
                createdAt,
                createdAt);
    }

    @Test
    void insertThenFindBySlugRoundTrips() {
        Problem problem = problem("two-sum", Difficulty.EASY, Set.of("arrays"), 1000L);
        repository.insert(problem);

        Optional<Problem> found = repository.findBySlug("two-sum");

        assertThat(found).contains(problem);
        assertThat(repository.existsBySlug("two-sum")).isTrue();
        assertThat(repository.existsBySlug("nope")).isFalse();
    }

    @Test
    void replaceOverwritesTheDocumentInPlace() {
        Problem original = problem("joins", Difficulty.MEDIUM, Set.of("sql"), 1000L);
        repository.insert(original);

        Problem updated = original.withPublished(false, 2000L);
        repository.replace(updated);

        assertThat(repository.findBySlug("joins")).contains(updated);
    }

    @Test
    void findPagePaginatesInCreatedAtOrderAndReportsHasMore() {
        for (int i = 0; i < 5; i++) {
            repository.insert(problem("p" + i, Difficulty.EASY, Set.of(), 1000L + i));
        }

        CursorPage<Problem> firstPage = repository.findPage(ProblemFilter.publishedOnly(), PageRequest.first(2));
        assertThat(firstPage.items()).extracting(Problem::slug).containsExactly("p0", "p1");
        assertThat(firstPage.hasMore()).isTrue();

        CursorPage<Problem> secondPage = repository.findPage(
                ProblemFilter.publishedOnly(), PageRequest.after(firstPage.nextCursor().orElseThrow(), 2));
        assertThat(secondPage.items()).extracting(Problem::slug).containsExactly("p2", "p3");
        assertThat(secondPage.hasMore()).isTrue();

        CursorPage<Problem> thirdPage = repository.findPage(
                ProblemFilter.publishedOnly(), PageRequest.after(secondPage.nextCursor().orElseThrow(), 2));
        assertThat(thirdPage.items()).extracting(Problem::slug).containsExactly("p4");
        assertThat(thirdPage.hasMore()).isFalse();
    }

    @Test
    void findPageFiltersByDifficultyTagAndPublished() {
        repository.insert(problem("easy-arrays", Difficulty.EASY, Set.of("arrays"), 1000L));
        repository.insert(problem("hard-arrays", Difficulty.HARD, Set.of("arrays"), 1001L));
        repository.insert(problem("easy-strings", Difficulty.EASY, Set.of("strings"), 1002L));
        repository.insert(problem("unpublished-easy", Difficulty.EASY, Set.of("arrays"), 1003L)
                .withPublished(false, 1003L));

        List<Problem> byDifficulty = repository.findPage(
                new ProblemFilter(Optional.empty(), Optional.of(Difficulty.EASY), Optional.empty(), Optional.empty(), true),
                PageRequest.first(10)).items();
        assertThat(byDifficulty).extracting(Problem::slug).containsExactlyInAnyOrder("easy-arrays", "easy-strings");

        List<Problem> byTag = repository.findPage(
                new ProblemFilter(Optional.of("arrays"), Optional.empty(), Optional.empty(), Optional.empty(), true),
                PageRequest.first(10)).items();
        assertThat(byTag).extracting(Problem::slug).containsExactlyInAnyOrder("easy-arrays", "hard-arrays");
    }

    @Test
    void listTagCountsAggregatesAcrossPublishedProblemsOnly() {
        repository.insert(problem("a", Difficulty.EASY, Set.of("arrays", "hash-map"), 1000L));
        repository.insert(problem("b", Difficulty.EASY, Set.of("arrays"), 1001L));
        repository.insert(problem("c", Difficulty.EASY, Set.of("arrays"), 1002L).withPublished(false, 1002L));

        List<TagCount> counts = new ArrayList<>(repository.listTagCounts(true));

        assertThat(counts).extracting(TagCount::tag).containsExactlyInAnyOrder("arrays", "hash-map");
        TagCount arrays = counts.stream().filter(c -> c.tag().equals("arrays")).findFirst().orElseThrow();
        assertThat(arrays.count()).isEqualTo(2L); // "c" is unpublished, excluded
    }
}
