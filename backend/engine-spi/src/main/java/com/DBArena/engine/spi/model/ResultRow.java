package com.DBArena.engine.spi.model;

import com.DBArena.common.core.value.CdmValue;

import java.util.List;

/** One row, column-order-aligned with the owning {@link ExecutionResult}'s column list. */
public record ResultRow(List<CdmValue> values) {

    public ResultRow {
        values = List.copyOf(values);
    }
}
