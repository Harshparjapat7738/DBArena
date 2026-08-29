package com.DBArena.engine.spi.model;

import java.time.Duration;

/**
 * One statement to run against a session: raw SQL text for Postgres, a
 * JSON command document for Mongo. Statement <em>validation</em> (AST-based,
 * per hard rule #4) happens before this reaches an adapter - by the time
 * execution-service builds a StatementRequest, the statement classifier
 * has already accepted it.
 */
public record StatementRequest(String statementText, Duration timeout) {

    public StatementRequest {
        if (statementText == null || statementText.isBlank()) {
            throw new IllegalArgumentException("statementText must not be blank");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
