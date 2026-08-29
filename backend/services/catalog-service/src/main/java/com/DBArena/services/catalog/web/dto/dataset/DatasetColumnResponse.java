package com.DBArena.services.catalog.web.dto.dataset;

import com.DBArena.services.catalog.domain.dataset.DatasetColumn;

public record DatasetColumnResponse(String name, String type, boolean nullable, boolean primaryKey, String foreignKey) {

    public static DatasetColumnResponse from(DatasetColumn column) {
        return new DatasetColumnResponse(column.name(), column.type(), column.nullable(), column.primaryKey(),
                column.foreignKey().orElse(null));
    }
}
