package com.dbforge.services.ai.provider;

/** One completion call, provider-agnostic. */
public record AiCompletionRequest(String systemPrompt, String userPrompt, int maxOutputTokens) {

    public AiCompletionRequest {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt must not be blank");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt must not be blank");
        }
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
    }
}
