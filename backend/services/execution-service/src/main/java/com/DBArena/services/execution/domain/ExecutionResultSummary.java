package com.DBArena.services.execution.domain;

import com.DBArena.engine.spi.model.ColumnMeta;

import java.util.List;

/**
 * A capped, display-ready result set - values are pre-stringified (never
 * the raw {@code CdmValue}/JDBC types) so this is trivially JSON/Mongo
 * serializable and never leaks an engine-native type across the wire.
 * {@code truncated} is {@code true} when more rows existed than
 * {@link ExecutionPolicy#maxResultRows()} allowed back.
 */
public record ExecutionResultSummary(List<ColumnMeta> columns, List<List<String>> rows, boolean truncated) {

    public ExecutionResultSummary {
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
    }
}
