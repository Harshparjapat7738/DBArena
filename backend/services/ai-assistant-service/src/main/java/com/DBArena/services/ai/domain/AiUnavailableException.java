package com.DBArena.services.ai.domain;

import com.DBArena.common.core.error.DomainException;

import java.util.Map;

/** Both the primary (Groq) and fallback (Gemini) providers failed for this request. */
public class AiUnavailableException extends DomainException {

    public AiUnavailableException(String primaryError, String fallbackError) {
        super("ai.unavailable", 502,
                "hint generation is temporarily unavailable - both AI providers failed",
                Map.of("primaryError", primaryError, "fallbackError", fallbackError));
    }
}
