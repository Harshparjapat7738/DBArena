package com.dbforge.engine.spi.model;

import com.dbforge.engine.spi.EngineType;

/**
 * Refers to one learner's live materialized copy of a dataset in one
 * engine. {@code connectionRef} is deliberately opaque here (a JDBC URL
 * alias for Postgres, a database name for Mongo) - callers outside the
 * adapter that issued it must treat it as a token, not parse it.
 */
public record SessionHandle(String sessionId, EngineType engineType, String connectionRef) {

    public SessionHandle {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (connectionRef == null || connectionRef.isBlank()) {
            throw new IllegalArgumentException("connectionRef must not be blank");
        }
    }
}
