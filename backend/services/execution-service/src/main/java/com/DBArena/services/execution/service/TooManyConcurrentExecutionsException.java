package com.DBArena.services.execution.service;

import com.DBArena.common.core.error.DomainException;

import java.util.Map;

/** The per-user or global concurrency ceiling (safe-default policy) is already at capacity - a resource control, not a validation failure. */
public class TooManyConcurrentExecutionsException extends DomainException {

    public TooManyConcurrentExecutionsException(String scope) {
        super("execution.too_many_concurrent", 429, "Too many concurrent executions (" + scope + ") - try again shortly", Map.of("scope", scope));
    }
}
