package com.dbforge.engine.adapters.mysql;

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
 * Reads back the live schema of a materialized MySQL database via
 * {@code information_schema} - proving what was actually created, rather
 * than trusting the {@link com.dbforge.engine.spi.cdm.CdmDataset} that was
 * asked for (needed for B06's cross-engine equivalence proof later, same
 * rationale as {@code PostgresIntrospector}). {@code column_type} (not
 * {@code data_type}) is used for each column's native type - see
 * {@link MySqlNativeTypes}'s Javadoc for why.
 *
 * <p>Also the single source of table-name and foreign-key metadata for
 * {@link MySqlTemplateCloner}, which needs to enumerate a template
 * database's tables and reconstruct its foreign keys without ever having
 * the original {@link com.dbforge.engine.spi.cdm.CdmDataset} in hand (a
 * {@code SessionHandle} carries only an opaque database-name reference) -
 * see {@link #tableNames} and {@link #foreignKeysOf}, both package-visible
 * for that reuse rather than private.
 */
final class MySqlIntrospector {

    private static final String TABLES_SQL = """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """;

    private static final String COLUMNS_SQL = """
            SELECT column_name, column_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position
            """;

    /**
     * Single-column foreign keys only (matches {@code CdmForeignKey}'s own
     * scope). {@code referenced_table_name IS NOT NULL} excludes plain
     * (non-FK) key-column-usage rows, e.g. a primary key's own entry in
     * this same catalog view.
     */
    private static final String FOREIGN_KEYS_SQL = """
            SELECT table_name, column_name, referenced_table_name, referenced_column_name
            FROM information_schema.key_column_usage
            WHERE table_schema = ? AND referenced_table_name IS NOT NULL
            ORDER BY table_name, column_name
            """;

    SchemaSnapshot introspect(Connection connection, String schemaName) {
        List<EntitySchema> entities = new ArrayList<>();
        for (String tableName : tableNames(connection, schemaName)) {
            entities.add(new EntitySchema(tableName, columnsOf(connection, schemaName, tableName)));
        }
        return new SchemaSnapshot(entities);
    }

    List<String> tableNames(Connection connection, String schemaName) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(TABLES_SQL)) {
            statement.setString(1, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString("table_name"));
                }
            }
            return names;
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to introspect table list for schema '" + schemaName + "'", e);
        }
    }

    List<ForeignKeyRef> foreignKeysOf(Connection connection, String schemaName) {
        List<ForeignKeyRef> foreignKeys = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FOREIGN_KEYS_SQL)) {
            statement.setString(1, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    foreignKeys.add(new ForeignKeyRef(
                            resultSet.getString("table_name"),
                            resultSet.getString("column_name"),
                            resultSet.getString("referenced_table_name"),
                            resultSet.getString("referenced_column_name")));
                }
            }
            return foreignKeys;
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to introspect foreign keys for schema '" + schemaName + "'", e);
        }
    }

    private List<ColumnMeta> columnsOf(Connection connection, String schemaName, String tableName) {
        List<ColumnMeta> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(COLUMNS_SQL)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String columnName = resultSet.getString("column_name");
                    String columnType = resultSet.getString("column_type");
                    boolean nullable = "YES".equalsIgnoreCase(resultSet.getString("is_nullable"));
                    CdmType cdmType = MySqlNativeTypes.fromColumnType(columnType).orElseThrow(() ->
                            new MySqlAdapterException(
                                    "Column '" + tableName + "." + columnName + "' has native type '" + columnType
                                            + "', which is not one this adapter ever materializes - "
                                            + "introspection found a schema this adapter did not create"));
                    columns.add(new ColumnMeta(columnName, cdmType.valueClass().getSimpleName(), nullable));
                }
            }
            return columns;
        } catch (SQLException e) {
            throw new MySqlAdapterException("Failed to introspect columns of '" + tableName + "'", e);
        }
    }

    /** One single-column foreign key as read back from {@code information_schema.key_column_usage}. */
    record ForeignKeyRef(String tableName, String columnName, String referencedTableName, String referencedColumnName) {
    }
}
