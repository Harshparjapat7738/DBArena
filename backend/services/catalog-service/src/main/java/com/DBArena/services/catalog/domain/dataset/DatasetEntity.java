package com.DBArena.services.catalog.domain.dataset;

import java.util.List;
import java.util.Map;

/**
 * One table/collection within a {@link DatasetMetadata}. {@code sampleRows}
 * values are stringified ({@code Map<String,String>}, {@code null} preserved
 * as a real {@code null}, not the string "null") rather than kept as
 * arbitrary typed values - this is display-only data for a schema-browsing
 * UI, never fed into a comparator or a query (hard rule #9 governs
 * decimal/timestamp *comparison*, which never happens here), so a single
 * simple representation beats reproducing CDM's full type system for a
 * preview table.
 */
public record DatasetEntity(
        String name,
        DatasetEntityKind kind,
        List<DatasetColumn> columns,
        List<Map<String, String>> sampleRows,
        List<DatasetRelationship> relationships) {

    public DatasetEntity {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        columns = List.copyOf(columns);
        sampleRows = List.copyOf(sampleRows);
        relationships = List.copyOf(relationships);
    }
}
