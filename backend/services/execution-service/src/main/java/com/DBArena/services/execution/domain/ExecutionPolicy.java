package com.DBArena.services.execution.domain;

import java.time.Duration;

/**
 * Safe-default policy (B04) - a plain, Spring-free mirror of
 * {@code ExecutionProperties.Policy} for the domain/service layer to depend
 * on instead of the config-annotated class directly. Every field here is a
 * server-side ceiling; nothing in the HTTP request body can raise any of
 * them - see {@code CreateExecutionRequest} (it has no field for any of
 * these) and root CLAUDE.md's "no client-supplied credentials" spirit
 * extended to policy in general.
 */
public record ExecutionPolicy(
        int maxStatementLength,
        int maxResultRows,
        long maxResultBytes,
        Duration statementTimeout,
        Duration explainTimeout,
        int maxConcurrentPerUser,
        int maxConcurrentGlobal,
        int sandboxConnectionLimit) {

    public ExecutionPolicy {
        if (maxStatementLength < 1) {
            throw new IllegalArgumentException("maxStatementLength must be positive");
        }
        if (maxResultRows < 1) {
            throw new IllegalArgumentException("maxResultRows must be positive");
        }
        if (statementTimeout == null || statementTimeout.isZero() || statementTimeout.isNegative()) {
            throw new IllegalArgumentException("statementTimeout must be positive");
        }
    }
}
