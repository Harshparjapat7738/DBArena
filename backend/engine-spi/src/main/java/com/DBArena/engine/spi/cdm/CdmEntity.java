package com.dbforge.engine.spi.cdm;

import java.util.List;
import java.util.Optional;

/** One table (Postgres/MySQL) or collection (Mongo) in a {@link CdmDataset}, authored once. */
public record CdmEntity(String name, List<CdmColumn> columns, List<CdmForeignKey> foreignKeys, List<CdmRow> seedRows) {

    public CdmEntity {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("entity name must not be blank");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("entity '" + name + "' must declare at least one column");
        }
        columns = List.copyOf(columns);
        foreignKeys = foreignKeys == null ? List.of() : List.copyOf(foreignKeys);
        seedRows = seedRows == null ? List.of() : List.copyOf(seedRows);
    }

    public Optional<CdmColumn> column(String columnName) {
        return columns.stream().filter(c -> c.name().equals(columnName)).findFirst();
    }

    public List<CdmColumn> primaryKeyColumns() {
        return columns.stream().filter(CdmColumn::primaryKey).toList();
    }
}
