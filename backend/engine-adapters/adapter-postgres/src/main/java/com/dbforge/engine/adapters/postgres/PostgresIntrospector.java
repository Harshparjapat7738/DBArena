package com.dbforge.engine.adapters.postgres;

import com.dbforge.engine.spi.cdm.CdmType;
import com.dbforge.engine.spi.model.ColumnMeta;
import com.dbforge.engine.spi.model.EntitySchema;
import com.dbforge.engine.spi.model.SchemaSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads back the live schema of a materialized Postgres database via
 * {@code information_schema} - proving what was actually created, rather
 * than trusting the {@link com.dbforge.engine.spi.cdm.CdmDataset} that was
 * asked for (needed for B06's cross-engine equivalence proof later).
 * {@code udt_name} (not {@code data_type}) is used for each column's
 * native type - see {@link PostgresNativeTypes}'s Javadoc for why.
 */
final class PostgresIntrospector {

    private static final String TABLES_SQL = """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """;

    private static final String COLUMNS_SQL = """
            SELECT column_name, udt_name, is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ?
            ORDER BY ordinal_position
            """;

    SchemaSnapshot introspect(Connection connection) {
        List<EntitySchema> entities = new ArrayList<>();
        for (String tableName : tableNames(connection)) {
            entities.add(new EntitySchema(tableName, columnsOf(connection, tableName)));
        }
        return new SchemaSnapshot(entities);
    }

    private List<String> tableNames(Connection connection) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(TABLES_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                names.add(resultSet.getString("table_name"));
            }
            return names;
        } catch (SQLException e) {
            throw new PostgresAdapterException("Failed to introspect table list", e);
        }
    }

    private List<ColumnMeta> columnsOf(Connection connection, String tableName) {
        List<ColumnMeta> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(COLUMNS_SQL)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String columnName = resultSet.getString("column_name");
                    String udtName = resultSet.getString("udt_name");
                    boolean nullable = "YES".equalsIgnoreCase(resultSet.getString("is_nullable"));
                    CdmType cdmType = PostgresNativeTypes.toCdmType(udtName).orElseThrow(() ->
                            new PostgresAdapterException(
                                    "Column '" + tableName + "." + columnName + "' has native type '" + udtName
                                            + "', which is not one this adapter ever materializes - "
                                            + "introspection found a schema this adapter did not create"));
                    columns.add(new ColumnMeta(columnName, cdmType.valueClass().getSimpleName(), nullable));
                }
            }
            return columns;
        } catch (SQLException e) {
            throw new PostgresAdapterException("Failed to introspect columns of '" + tableName + "'", e);
        }
    }
}
