package com.DBArena.engine.spi.model;

import java.time.Duration;
import java.util.Optional;

/**
 * One statement to run against a session: raw SQL text for Postgres, a
 * JSON command document for Mongo. Statement <em>validation</em> (AST-based,
 * per hard rule #4) happens before this reaches an adapter - by the time
 * execution-service builds a StatementRequest, the statement classifier
 * has already accepted it.
 *
 * <p>{@code maxRows} (B04, execution-service) is the JDBC-level row cap a
 * caller wants enforced server-side (not client-supplied - execution-service
 * derives it from its own {@code ExecutionPolicy}, never from the HTTP
 * request body) - {@link Optional#empty()} means unbounded, same as JDBC's
 * {@code Statement.setMaxRows(0)}.
 */
public record StatementRequest(String statementText, Duration timeout, Optional<Integer> maxRows) {

    public StatementRequest {
        if (statementText == null || statementText.isBlank()) {
            throw new IllegalArgumentException("statementText must not be blank");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        maxRows = maxRows == null ? Optional.empty() : maxRows;
        if (maxRows.isPresent() && maxRows.get() < 1) {
            throw new IllegalArgumentException("maxRows must be >= 1 when present");
        }
    }

    /** Back-compat convenience constructor (B04) - unbounded rows, same as every pre-B04 call site expected. */
    public StatementRequest(String statementText, Duration timeout) {
        this(statementText, timeout, Optional.empty());
    }
}
