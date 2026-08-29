package com.DBArena.services.ai.prompt;

import com.DBArena.services.ai.context.ColumnSummary;
import com.DBArena.services.ai.context.EntitySummary;
import com.DBArena.services.ai.context.HintContext;
import com.DBArena.services.ai.domain.HintLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HintPromptBuilderTest {

    private final HintPromptBuilder builder = new HintPromptBuilder();

    @Test
    void systemPromptNamesTheRequestedLevel() {
        String prompt = builder.buildSystemPrompt(HintLevel.NEAR_MISS);

        assertThat(prompt).contains("NEAR_MISS-level hint");
    }

    @Test
    void systemPromptForbidsACompleteRunnableQuery() {
        String prompt = builder.buildSystemPrompt(HintLevel.CONCEPT);

        assertThat(prompt).contains("Never write a complete, directly runnable final query");
    }

    @Test
    void systemPromptForbidsClaimingAReferenceSolution() {
        String prompt = builder.buildSystemPrompt(HintLevel.APPROACH);

        assertThat(prompt).contains("reference solution");
    }

    @Test
    void systemPromptStatesAWordLimit() {
        String prompt = builder.buildSystemPrompt(HintLevel.CONCEPT);

        assertThat(prompt).containsPattern("Hard limit: \\d+ words");
    }

    @Test
    void userPromptIncludesEveryContextSection() {
        HintContext context = new HintContext(
                "Two Sum", "Find two numbers...", "EASY", Set.of("arrays"), Set.of("POSTGRES"),
                List.of(new EntitySummary("numbers",
                        List.of(new ColumnSummary("id", "INTEGER", false, true)),
                        List.of(Map.of("id", "1")))),
                "SELECT * FROM numbers", "syntax error near FROM", HintLevel.NEAR_MISS);

        String prompt = builder.buildUserPrompt(context);

        assertThat(prompt).contains("Two Sum").contains("EASY").contains("arrays")
                .contains("Find two numbers...")
                .contains("numbers(id:INTEGER PK NOT NULL)")
                .contains("SELECT * FROM numbers")
                .contains("syntax error near FROM")
                .contains("NEAR_MISS");
    }

    @Test
    void userPromptOmitsTheErrorSectionWhenThereIsNone() {
        HintContext context = new HintContext(
                "Two Sum", "stmt", "EASY", Set.of(), Set.of(), List.of(),
                "SELECT 1", "", HintLevel.CONCEPT);

        String prompt = builder.buildUserPrompt(context);

        assertThat(prompt).doesNotContain("LEARNER_ERROR_OR_RESULT");
    }

    @Test
    void userPromptOmitsTheSchemaSectionWhenThereIsNoDataset() {
        HintContext context = new HintContext(
                "Two Sum", "stmt", "EASY", Set.of(), Set.of(), List.of(),
                "SELECT 1", null, HintLevel.CONCEPT);

        String prompt = builder.buildUserPrompt(context);

        assertThat(prompt).doesNotContain("SCHEMA:");
    }
}
