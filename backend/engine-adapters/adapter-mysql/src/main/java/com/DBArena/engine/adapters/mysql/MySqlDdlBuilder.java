package com.dbforge.engine.adapters.mysql;

import com.dbforge.engine.spi.cdm.CdmColumn;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.cdm.CdmEntity;
import com.dbforge.engine.spi.cdm.CdmForeignKey;
import com.dbforge.engine.spi.typemap.MySqlColumnType;
import com.dbforge.engine.spi.typemap.MySqlTypeMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure {@link CdmDataset} -&gt; DDL text translation - no JDBC here, so it is
 * trivially unit-testable without a database. Mirrors {@code
 * PostgresDdlBuilder}'s two-pass structure exactly: every entity's table is
 * created first, without foreign keys; every foreign key across every
 * entity is added afterward via {@link #addForeignKeyStatements}, once
 * every table exists, so entity declaration order in the dataset never has
 * to be a topological sort over the foreign-key graph.
 *
 * <p>{@code ENGINE=InnoDB} is stated explicitly on every {@code CREATE
 * TABLE} - InnoDB is MySQL 8's default storage engine already, but foreign
 * keys (added in the second pass) require InnoDB specifically (MyISAM,
 * MySQL's other historically common engine, silently accepts and ignores
 * {@code ADD CONSTRAINT ... FOREIGN KEY} without enforcing it) - stating it
 * removes any dependency on the server's configured default.
 */
final class MySqlDdlBuilder {

    private static final MySqlTypeMapper TYPE_MAPPER = new MySqlTypeMapper();

    private MySqlDdlBuilder() {
    }

    static List<String> createTableStatements(CdmDataset dataset) {
        List<String> statements = new ArrayList<>();
        for (CdmEntity entity : dataset.entities()) {
            statements.add(createTableStatement(entity));
        }
        return statements;
    }

    static List<String> addForeignKeyStatements(CdmEntity entity) {
        List<String> statements = new ArrayList<>();
        for (CdmForeignKey fk : entity.foreignKeys()) {
            statements.add(
                    "ALTER TABLE " + MySqlIdentifiers.quote(entity.name())
                            + " ADD CONSTRAINT " + MySqlIdentifiers.quote(constraintName(entity.name(), fk))
                            + " FOREIGN KEY (" + MySqlIdentifiers.quote(fk.columnName()) + ")"
                            + " REFERENCES " + MySqlIdentifiers.quote(fk.referencesEntity())
                            + " (" + MySqlIdentifiers.quote(fk.referencesColumn()) + ")");
        }
        return statements;
    }

    private static String createTableStatement(CdmEntity entity) {
        List<String> parts = new ArrayList<>();
        for (CdmColumn column : entity.columns()) {
            parts.add(columnDefinition(column));
        }
        List<String> pkColumnNames = entity.primaryKeyColumns().stream()
                .map(c -> MySqlIdentifiers.quote(c.name()))
                .toList();
        if (!pkColumnNames.isEmpty()) {
            parts.add("PRIMARY KEY (" + String.join(", ", pkColumnNames) + ")");
        }
        return "CREATE TABLE " + MySqlIdentifiers.quote(entity.name())
                + " (\n  " + String.join(",\n  ", parts) + "\n)"
                + " ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin";
    }

    private static String columnDefinition(CdmColumn column) {
        MySqlColumnType type = TYPE_MAPPER.map(column.type());
        StringBuilder def = new StringBuilder()
                .append(MySqlIdentifiers.quote(column.name()))
                .append(' ')
                .append(type.sqlTypeName());
        if (type == MySqlColumnType.TEXT) {
            // Hard rule #9: collations are pinned. utf8mb4_bin is a byte-for-byte
            // binary collation - no case-insensitive or accent-insensitive comparison
            // sneaking into a result set ordering or comparison a learner's query relies on.
            def.append(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin");
        }
        if (!column.nullable()) {
            def.append(" NOT NULL");
        }
        return def.toString();
    }

    /** {@code fk_<entity>_<column>}, truncated to MySQL's identifier limit - same known, accepted long-name-collision edge case as PostgresDdlBuilder. */
    private static String constraintName(String entityName, CdmForeignKey fk) {
        String raw = "fk_" + entityName + "_" + fk.columnName();
        return raw.length() > MySqlIdentifiers.MYSQL_IDENTIFIER_LIMIT
                ? raw.substring(0, MySqlIdentifiers.MYSQL_IDENTIFIER_LIMIT)
                : raw;
    }
}
