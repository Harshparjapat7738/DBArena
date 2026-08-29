package com.dbforge.engine.adapters.postgres;

import com.dbforge.engine.spi.cdm.CdmColumn;
import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.cdm.CdmEntity;
import com.dbforge.engine.spi.cdm.CdmRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs one {@link CdmDataset}'s DDL and seed rows against an
 * already-created, empty Postgres database (see
 * {@link PostgresEngineAdapter#materialize} for how that database gets
 * created). Every entity's table is created first, without foreign keys;
 * every foreign key across every entity is added afterward - see {@link
 * PostgresDdlBuilder}'s Javadoc for why.
 */
final class PostgresMaterializer {

    Map<String, Long> materialize(Connection connection, CdmDataset dataset) {
        runDdl(connection, dataset);

        Map<String, Long> rowCounts = new LinkedHashMap<>();
        for (CdmEntity entity : dataset.entities()) {
            rowCounts.put(entity.name(), seedEntity(connection, entity));
        }
        return rowCounts;
    }

    private void runDdl(Connection connection, CdmDataset dataset) {
        try (Statement statement = connection.createStatement()) {
            for (String ddl : PostgresDdlBuilder.createTableStatements(dataset)) {
                statement.execute(ddl);
            }
            for (CdmEntity entity : dataset.entities()) {
                for (String fkDdl : PostgresDdlBuilder.addForeignKeyStatements(entity)) {
                    statement.execute(fkDdl);
                }
            }
        } catch (SQLException e) {
            throw new PostgresAdapterException(
                    "Failed to materialize schema for dataset '" + dataset.datasetId() + "'", e);
        }
    }

    private long seedEntity(Connection connection, CdmEntity entity) {
        if (entity.seedRows().isEmpty()) {
            return 0L;
        }

        String columnList = entity.columns().stream()
                .map(c -> PostgresIdentifiers.quote(c.name()))
                .collect(Collectors.joining(", "));
        String placeholders = entity.columns().stream().map(c -> "?").collect(Collectors.joining(", "));
        String insertSql = "INSERT INTO " + PostgresIdentifiers.quote(entity.name())
                + " (" + columnList + ") VALUES (" + placeholders + ")";

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (CdmRow row : entity.seedRows()) {
                int index = 1;
                for (CdmColumn column : entity.columns()) {
                    CdmValueJdbcCodec.bind(statement, index++, column.type(), row.get(column.name()));
                }
                statement.addBatch();
            }
            return statement.executeBatch().length;
        } catch (SQLException e) {
            throw new PostgresAdapterException("Failed to seed entity '" + entity.name() + "'", e);
        }
    }
}
