package com.DBArena.engine.adapters.postgres;

/**
 * Safe double-quoted identifier rendering for every table/column/constraint
 * name this module writes into DDL. Every identifier is quoted
 * unconditionally (not just ones that clash with a SQL keyword or use
 * mixed case) so there is exactly one code path, not "quote sometimes".
 */
final class PostgresIdentifiers {

    private static final int POSTGRES_NAMEDATALEN_LIMIT = 63;

    private PostgresIdentifiers() {
    }

    /**
     * Quotes {@code identifier} for use in DDL/DML. Rejects a name
     * containing a double quote outright rather than escaping it - a CDM
     * entity/column name is admin-authored today (no validator restricts
     * its character set - see {@code CdmDatasetValidator}), so this is
     * defense-in-depth against a malformed dataset breaking out of the
     * quoting, the same caution hard rule #4 requires for learner SQL
     * applied here too, cheaply.
     */
    static String quote(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier must not be blank");
        }
        if (identifier.contains("\"")) {
            throw new IllegalArgumentException("identifier must not contain a double quote: " + identifier);
        }
        if (identifier.length() > POSTGRES_NAMEDATALEN_LIMIT) {
            throw new IllegalArgumentException(
                    "identifier exceeds Postgres's " + POSTGRES_NAMEDATALEN_LIMIT + "-byte NAMEDATALEN limit: " + identifier);
        }
        return "\"" + identifier + "\"";
    }
}
