package com.DBArena.engine.adapters.postgres;

import com.DBArena.engine.spi.cdm.CdmColumn;
import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.engine.spi.cdm.CdmEntity;
import com.DBArena.engine.spi.cdm.CdmForeignKey;
import com.DBArena.engine.spi.typemap.PostgresColumnType;
import com.DBArena.engine.spi.typemap.PostgresTypeMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure {@link CdmDataset} -&gt; DDL text translation - no JDBC here, so it is
 * trivially unit-testable without a database. Tables are generated without
 * foreign keys first; {@link #addForeignKeyStatements} is applied
 * afterward, once every table exists, so entity declaration order in the
 * dataset never has to be a topological sort over the foreign-key graph.
 */
final class PostgresDdlBuilder {

    private static final PostgresTypeMapper TYPE_MAPPER = new PostgresTypeMapper();
    private static final int MAX_IDENTIFIER_LENGTH = 63;

    private PostgresDdlBuilder() {
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
                    "ALTER TABLE " + PostgresIdentifiers.quote(entity.name())
                            + " ADD CONSTRAINT " + PostgresIdentifiers.quote(constraintName(entity.name(), fk))
                            + " FOREIGN KEY (" + PostgresIdentifiers.quote(fk.columnName()) + ")"
                            + " REFERENCES " + PostgresIdentifiers.quote(fk.referencesEntity())
                            + " (" + PostgresIdentifiers.quote(fk.referencesColumn()) + ")");
        }
        return statements;
    }

    private static String createTableStatement(CdmEntity entity) {
        List<String> parts = new ArrayList<>();
        for (CdmColumn column : entity.columns()) {
            parts.add(columnDefinition(column));
        }
        List<String> pkColumnNames = entity.primaryKeyColumns().stream()
                .map(c -> PostgresIdentifiers.quote(c.name()))
                .toList();
        if (!pkColumnNames.isEmpty()) {
            parts.add("PRIMARY KEY (" + String.join(", ", pkColumnNames) + ")");
        }
        return "CREATE TABLE " + PostgresIdentifiers.quote(entity.name())
                + " (\n  " + String.join(",\n  ", parts) + "\n)";
    }

    private static String columnDefinition(CdmColumn column) {
        PostgresColumnType pgType = TYPE_MAPPER.map(column.type());
        StringBuilder def = new StringBuilder()
                .append(PostgresIdentifiers.quote(column.name()))
                .append(' ')
                .append(pgType.sqlTypeName());
        if (pgType == PostgresColumnType.TEXT) {
            // Hard rule #9: collations are pinned. C ships with every Postgres install
            // (glibc or musl-based alike), unlike a locale-specific collation.
            def.append(" COLLATE \"C\"");
        }
        if (!column.nullable()) {
            def.append(" NOT NULL");
        }
        return def.toString();
    }

    /** {@code fk_<entity>_<column>}, truncated to Postgres's identifier limit - a long-name collision is a known, accepted edge case for now. */
    private static String constraintName(String entityName, CdmForeignKey fk) {
        String raw = "fk_" + entityName + "_" + fk.columnName();
        return raw.length() > MAX_IDENTIFIER_LENGTH ? raw.substring(0, MAX_IDENTIFIER_LENGTH) : raw;
    }
}
