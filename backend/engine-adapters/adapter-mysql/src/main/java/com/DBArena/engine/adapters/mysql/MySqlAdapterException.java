package com.DBArena.engine.adapters.mysql;

/**
 * Wraps a checked {@link java.sql.SQLException} (or another
 * adapter-internal failure) as unchecked - none of
 * {@link com.DBArena.engine.spi.DatabaseEngineAdapter}'s methods declare
 * checked exceptions. Not used by {@link MySqlEngineAdapter#execute}, which
 * never throws for a normal query failure (see its own Javadoc) - this is
 * for everything else: connection failures, DDL/materialization/clone
 * failures, and any other condition that is a bug or an infrastructure
 * problem, not "the learner's SQL was wrong". Mirrors
 * {@code PostgresAdapterException} exactly.
 */
public class MySqlAdapterException extends RuntimeException {

    public MySqlAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    public MySqlAdapterException(String message) {
        super(message);
    }
}
