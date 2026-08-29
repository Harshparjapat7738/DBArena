package com.DBArena.engine.adapters.mysql;

/**
 * Safe backtick-quoted identifier rendering for every database/table/
 * column/constraint name this module writes into DDL/DML - MySQL's
 * quoting character, unlike Postgres's double quote. Every identifier is
 * quoted unconditionally (not just ones that clash with a SQL keyword or
 * use mixed case) so there is exactly one code path, not "quote
 * sometimes" - mirrors {@code PostgresIdentifiers} exactly, including its
 * "reject rather than escape" posture on an embedded quote character.
 */
final class MySqlIdentifiers {

    /** MySQL's identifier length limit (database, table, column, and constraint names alike) - 64 characters, not bytes. Package-visible: also used by {@link MySqlDdlBuilder}'s constraint-name truncation. */
    static final int MYSQL_IDENTIFIER_LIMIT = 64;

    private MySqlIdentifiers() {
    }

    /**
     * Quotes {@code identifier} for use in DDL/DML. Rejects a name
     * containing a backtick outright rather than doubling it - a CDM
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
        if (identifier.contains("`")) {
            throw new IllegalArgumentException("identifier must not contain a backtick: " + identifier);
        }
        if (identifier.length() > MYSQL_IDENTIFIER_LIMIT) {
            throw new IllegalArgumentException(
                    "identifier exceeds MySQL's " + MYSQL_IDENTIFIER_LIMIT + "-character limit: " + identifier);
        }
        return "`" + identifier + "`";
    }

    /** {@code `schema`.`table`} - every cross-schema statement in this module (templateClone's table copy) needs both parts quoted independently. */
    static String qualify(String schemaName, String objectName) {
        return quote(schemaName) + "." + quote(objectName);
    }
}
