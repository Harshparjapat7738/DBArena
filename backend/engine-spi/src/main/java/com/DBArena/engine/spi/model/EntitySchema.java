package com.dbforge.engine.spi.model;

import java.util.List;

/** One table (Postgres) or collection (Mongo) as introspected from a materialized session. */
public record EntitySchema(String name, List<ColumnMeta> columns) {

    public EntitySchema {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        columns = List.copyOf(columns);
    }
}
