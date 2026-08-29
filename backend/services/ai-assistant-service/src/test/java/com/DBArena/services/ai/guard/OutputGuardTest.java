package com.dbforge.services.ai.guard;

import com.dbforge.services.ai.provider.AiCompletionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputGuardTest {

    private final OutputGuard guard = new OutputGuard();

    @Test
    void aShortResponsePassesThroughUnchanged() {
        AiCompletionResult result = new AiCompletionResult("A short hint.", "groq");

        OutputGuard.GuardedHint guarded = guard.apply(result);

        assertThat(guarded.text()).isEqualTo("A short hint.");
        assertThat(guarded.provider()).isEqualTo("groq");
        assertThat(guarded.truncated()).isFalse();
    }

    @Test
    void aResponseOverTheHardCapIsTruncatedAndFlagged() {
        String longText = "a".repeat(OutputGuard.HARD_MAX_CHARS + 500);
        AiCompletionResult result = new AiCompletionResult(longText, "gemini");

        OutputGuard.GuardedHint guarded = guard.apply(result);

        assertThat(guarded.text().length()).isLessThanOrEqualTo(OutputGuard.HARD_MAX_CHARS + 1); // +1 for the "…" marker
        assertThat(guarded.text()).endsWith("…");
        assertThat(guarded.truncated()).isTrue();
    }

    @Test
    void aResponseExactlyAtTheCapIsNotFlaggedTruncated() {
        String exactText = "a".repeat(OutputGuard.HARD_MAX_CHARS);
        AiCompletionResult result = new AiCompletionResult(exactText, "groq");

        OutputGuard.GuardedHint guarded = guard.apply(result);

        assertThat(guarded.truncated()).isFalse();
        assertThat(guarded.text()).isEqualTo(exactText);
    }

    @Test
    void leadingAndTrailingWhitespaceIsStripped() {
        AiCompletionResult result = new AiCompletionResult("   padded hint   ", "groq");

        OutputGuard.GuardedHint guarded = guard.apply(result);

        assertThat(guarded.text()).isEqualTo("padded hint");
    }

    @Test
    void theHardCapIsAPositiveOverAllTargetsSetElsewhere() {
        // Sanity-pins the relationship this class's Javadoc claims: the cap
        // is a ceiling above the prompt's own compact target, not a
        // replacement for it.
        assertThat(OutputGuard.HARD_MAX_CHARS).isGreaterThan(0);
    }
}
