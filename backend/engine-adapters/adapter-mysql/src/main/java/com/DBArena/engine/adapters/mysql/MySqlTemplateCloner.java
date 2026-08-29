package com.dbforge.engine.adapters.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * The MySQL equivalent of Postgres's {@code CREATE DATABASE ... TEMPLATE
 * ...} - which MySQL has no native primitive for at all. Unlike Postgres
 * (where {@code PostgresEngineAdapter#templateClone} is a single DDL
 * statement), cloning a template database here means, against one
 * connection with no default schema selected (fully qualified
 * {@code `schema`.`table`} names throughout, since a MySQL connection can
 * read/write any schema its user has privileges on without switching a
 * "current" database - see {@link MySqlConnectionFactory}'s Javadoc):
 *
 * <ol>
 *   <li>{@code CREATE TABLE clone.t LIKE template.t} for every table -
 *   copies columns, indexes, and the primary key, but <strong>not</strong>
 *   foreign keys (long-standing, version-independent MySQL {@code CREATE
 *   TABLE ... LIKE} behavior) - so foreign keys are deliberately not
 *   expected to appear yet after this step.</li>
 *   <li>{@code INSERT INTO clone.t SELECT * FROM template.t} for every
 *   table, with {@code FOREIGN_KEY_CHECKS} disabled for the duration - the
 *   tables don't have their foreign keys back yet (previous step), and
 *   even once they do, copying in an arbitrary table order must not fail
 *   because a child table's rows were inserted before its parent's.</li>
 *   <li>Foreign keys are added back explicitly afterward, via {@link
 *   MySqlIntrospector#foreignKeysOf} against the <em>template</em> schema
 *   (the only place they still exist - see step 1) and {@code ALTER TABLE
 *   ... ADD CONSTRAINT} against the newly-populated clone. Adding a
 *   constraint after the data is already present still validates every
 *   existing row against it - safe here only because the clone's data is a
 *   faithful copy of data that already satisfied the same constraint in
 *   the template.</li>
 * </ol>
 */
final class MySqlTemplateCloner {

    private final MySqlIntrospector introspector = new MySqlIntrospector();

    void clone(Connection connection, String templateSchema, String newSchema) {
        List<String> tables = introspector.tableNames(connection, templateSchema);

        try (Statement statement = connection.createStatement()) {
            for (String table : tables) {
                statement.execute("CREATE TABLE " + MySqlIdentifiers.qualify(newSchema, table)
                        + " LIKE " + MySqlIdentifiers.qualify(templateSchema, table));
            }

            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            try {
                for (String table : tables) {
                    statement.execute("INSERT INTO " + MySqlIdentifiers.qualify(newSchema, table)
                            + " SELECT * FROM " + MySqlIdentifiers.qualify(templateSchema, table));
                }
                for (MySqlIntrospector.ForeignKeyRef fk : introspector.foreignKeysOf(connection, templateSchema)) {
                    statement.execute("ALTER TABLE " + MySqlIdentifiers.qualify(newSchema, fk.tableName())
                            + " ADD CONSTRAINT " + MySqlIdentifiers.quote(constraintName(newSchema, fk))
                            + " FOREIGN KEY (" + MySqlIdentifiers.quote(fk.columnName()) + ")"
                            + " REFERENCES " + MySqlIdentifiers.qualify(newSchema, fk.referencedTableName())
                            + " (" + MySqlIdentifiers.quote(fk.referencedColumnName()) + ")");
                }
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        } catch (SQLException e) {
            throw new MySqlAdapterException(
                    "Failed to clone template database '" + templateSchema + "' into '" + newSchema + "'", e);
        }
    }

    /** {@code fk_<schema>_<table>_<column>} - namespaced by the clone's own schema name so a re-added constraint name never collides with the template's. */
    private static String constraintName(String newSchema, MySqlIntrospector.ForeignKeyRef fk) {
        String raw = "fk_" + newSchema + "_" + fk.tableName() + "_" + fk.columnName();
        return raw.length() > MySqlIdentifiers.MYSQL_IDENTIFIER_LIMIT
                ? raw.substring(0, MySqlIdentifiers.MYSQL_IDENTIFIER_LIMIT)
                : raw;
    }
}
