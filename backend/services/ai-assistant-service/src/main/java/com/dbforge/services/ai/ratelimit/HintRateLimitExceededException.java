package com.dbforge.services.ai.ratelimit;

import com.dbforge.common.core.error.DomainException;

import java.time.Duration;

public class HintRateLimitExceededException extends DomainException {

    public HintRateLimitExceededException(int maxRequestsPerWindow, Duration window) {
        super("ai.rate_limit_exceeded", 429,
                "Hint limit reached: " + maxRequestsPerWindow + " per " + window.toHours()
                        + "h. Try again later.");
    }
}
