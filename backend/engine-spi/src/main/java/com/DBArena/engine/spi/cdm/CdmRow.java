package com.DBArena.engine.spi.cdm;

import com.DBArena.common.core.value.CdmValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One seed row: column name -> value. Deliberately backed by a
 * {@link LinkedHashMap} wrapped unmodifiable rather than {@code Map.copyOf}
 * - column order is preserved so a materializer can render an
 * {@code INSERT} with a stable, human-reviewable column list instead of
 * whatever order a hash map happens to iterate in.
 */
public record CdmRow(Map<String, CdmValue> values) {

    public CdmRow {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public CdmValue get(String columnName) {
        CdmValue value = values.get(columnName);
        return value == null ? CdmValue.Null.INSTANCE : value;
    }
}
