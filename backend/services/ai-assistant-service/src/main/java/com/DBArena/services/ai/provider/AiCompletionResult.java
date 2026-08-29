package com.DBArena.services.ai.provider;

/** {@code providerName} is which provider actually answered ("groq" or "gemini") - surfaced to the caller for observability, never hidden. */
public record AiCompletionResult(String text, String providerName) {

    public AiCompletionResult {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("providerName must not be blank");
        }
    }
}
