package com.DBArena.services.catalog.service;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.catalog.domain.Difficulty;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.Problem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedProblemsRankerTest {

    private static Problem problem(String slug, String datasetSlug, String... tags) {
        return new Problem(TypedId.of("id-" + slug), slug, "Title " + slug, "Statement", Difficulty.EASY,
                Set.of(tags), Set.of(EngineType.POSTGRES), datasetSlug, true, 1L, 1L);
    }

    @Test
    void sameDatasetOutranksASharedTagAlone() {
        Problem subject = problem("two-sum", "sales-dataset", "arrays", "hash-map");
        Problem sameDataset = problem("top-spenders", "sales-dataset"); // +2, no shared tags
        Problem sharedTagOnly = problem("array-basics", "other-dataset", "arrays"); // +1

        List<Problem> ranked = RelatedProblemsRanker.rank(subject, List.of(sharedTagOnly, sameDataset), 10);

        assertThat(ranked).containsExactly(sameDataset, sharedTagOnly);
    }

    @Test
    void zeroScoreCandidatesAreExcluded() {
        Problem subject = problem("two-sum", "sales-dataset", "arrays");
        Problem unrelated = problem("joins-101", "other-dataset", "joins");

        List<Problem> ranked = RelatedProblemsRanker.rank(subject, List.of(unrelated), 10);

        assertThat(ranked).isEmpty();
    }

    @Test
    void resultIsCappedAtLimit() {
        Problem subject = problem("two-sum", "sales-dataset", "arrays");
        List<Problem> candidates = List.of(
                problem("a", "sales-dataset"), problem("b", "sales-dataset"), problem("c", "sales-dataset"));

        List<Problem> ranked = RelatedProblemsRanker.rank(subject, candidates, 2);

        assertThat(ranked).hasSize(2);
    }
}
