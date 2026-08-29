package com.dbforge.engine.spi.model;

import com.dbforge.common.core.value.CdmValue;

import java.util.List;

/** One row, column-order-aligned with the owning {@link ExecutionResult}'s column list. */
public record ResultRow(List<CdmValue> values) {

    public ResultRow {
        values = List.copyOf(values);
    }
}
