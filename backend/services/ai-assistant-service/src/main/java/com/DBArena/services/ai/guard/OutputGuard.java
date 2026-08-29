package com.DBArena.services.ai.guard;

import com.DBArena.services.ai.provider.AiCompletionResult;
import org.springframework.stereotype.Component;

/**
 * Server-side enforcement of "output must be within the output range" -
 * the prompt asks the model for a word-capped hint ({@code
 * HintPromptBuilder}) and the request asks the provider for a bounded
 * token count ({@code AiProviderProperties#maxOutputTokens}), but neither
 * is a guarantee: a model can ignore instructions. This class is the
 * actual guarantee, applied after the fact to whatever came back,
 * regardless of provider or prompt - the same "don't trust the input,
 * verify it yourself" posture hard rule #5 already takes for context size.
 *
 * <p>{@link #HARD_MAX_CHARS} is a ceiling on top of the already-compact
 * target, not a substitute for it - a model that mostly obeys the prompt
 * should never actually hit this cap; it exists for the case where one
 * doesn't.
 */
@Component
public class OutputGuard {

    /** Absolute ceiling, independent of any configuration - never overridable, same spirit as hard rule #5's row cap. */
    public static final int HARD_MAX_CHARS = 1200;

    public GuardedHint apply(AiCompletionResult result) {
        String text = result.text().strip();
        boolean truncated = false;
        if (text.length() > HARD_MAX_CHARS) {
            text = text.substring(0, HARD_MAX_CHARS).strip() + "…";
            truncated = true;
        }
        return new GuardedHint(text, result.providerName(), truncated);
    }

    public record GuardedHint(String text, String provider, boolean truncated) {
    }
}
