package com.dbforge.services.ai.provider;

import com.dbforge.services.ai.domain.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tries Groq first, falls back to Gemini if Groq is unconfigured or fails
 * for any reason (timeout, non-2xx, unparseable response). Constructor
 * injection of the two concrete clients (not a {@code List<AiCompletionClient>})
 * is deliberate - it pins "Groq is primary, Gemini is fallback" as a
 * compile-time-visible fact, not an ordering that depends on Spring bean
 * registration order or a {@code @Order} annotation someone could get
 * backwards.
 */
@Component
public class FallbackAiCompletionGateway {

    private static final Logger log = LoggerFactory.getLogger(FallbackAiCompletionGateway.class);

    private final GroqCompletionClient primary;
    private final GeminiCompletionClient fallback;

    public FallbackAiCompletionGateway(GroqCompletionClient primary, GeminiCompletionClient fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    public AiCompletionResult complete(AiCompletionRequest request) {
        String primaryError;
        try {
            return primary.complete(request);
        } catch (AiProviderException e) {
            primaryError = e.getMessage();
            log.warn("primary AI provider ({}) failed, falling back: {}", primary.name(), primaryError);
        }

        String fallbackError;
        try {
            return fallback.complete(request);
        } catch (AiProviderException e) {
            fallbackError = e.getMessage();
            log.error("fallback AI provider ({}) also failed: {}", fallback.name(), fallbackError);
        }

        throw new AiUnavailableException(primaryError, fallbackError);
    }
}
