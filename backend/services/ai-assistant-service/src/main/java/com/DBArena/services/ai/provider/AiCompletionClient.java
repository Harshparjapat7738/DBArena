package com.DBArena.services.ai.provider;

/** One LLM provider's chat-completion call, reduced to exactly what a hint needs. */
public interface AiCompletionClient {

    /** Stable lowercase name used in logs and in {@link AiCompletionResult#providerName()} ("groq", "gemini"). */
    String name();

    /** True if this client has enough configuration (an API key) to even attempt a call. */
    boolean configured();

    /** @throws AiProviderException on any failure - not configured, network/timeout, non-2xx, or unparseable response. */
    AiCompletionResult complete(AiCompletionRequest request);
}
