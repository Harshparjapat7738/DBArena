package com.DBArena.services.execution.sandbox;

import com.DBArena.common.core.error.DomainException;

import java.util.Map;

/**
 * "No production DB" as a hard runtime assertion, not just a design
 * intention: if a {@link com.DBArena.engine.spi.model.SessionHandle} ever
 * points somewhere other than a name this service itself would have
 * generated, refuse to touch it rather than trust the caller. Should never
 * actually trigger - it exists as a last-resort backstop against a future
 * bug elsewhere in the session-handling chain pointing execution at the
 * wrong database.
 */
public class NotASandboxDatabaseException extends DomainException {

    public NotASandboxDatabaseException(String connectionRef) {
        super("execution.not_a_sandbox_database", 500,
                "Refusing to execute against a database outside this service's own sandbox naming convention",
                Map.of("connectionRef", connectionRef));
    }
}
