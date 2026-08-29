package com.DBArena.services.ai.domain;

/**
 * The already-guarded (length-capped, provider-attributed) result of one
 * hint call, before web-layer serialization.
 */
public record HintResult(String problemSlug, HintLevel level, String hint, String provider, boolean truncated) {

    public HintResult {
        if (hint == null || hint.isBlank()) {
            throw new IllegalArgumentException("hint must not be blank");
        }
    }
}
