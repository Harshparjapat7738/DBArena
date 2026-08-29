package com.DBArena.engine.spi.cdm;

import com.DBArena.common.core.value.CdmValue;

/**
 * The declared type of one CDM column. Deliberately a closed, 1:1 mirror of
 * {@link CdmValue}'s sealed variants (minus {@code Null}, which isn't a
 * type - it's the absence of a value, expressed on {@link CdmColumn} via
 * {@code nullable} instead) - see {@link #valueClass()}. Every future
 * type-mapping table (B03: {@code CdmType} -> Postgres/Mongo native types)
 * switches on this enum, never on a raw string.
 */
public enum CdmType {
    BOOLEAN(CdmValue.Bool.class),
    INTEGER(CdmValue.Int.class),
    DECIMAL(CdmValue.Decimal.class),
    TEXT(CdmValue.Text.class),
    TIMESTAMP(CdmValue.Timestamp.class),
    JSON(CdmValue.Json.class);

    private final Class<? extends CdmValue> valueClass;

    CdmType(Class<? extends CdmValue> valueClass) {
        this.valueClass = valueClass;
    }

    /** The {@link CdmValue} record variant a value of this type must be an instance of. */
    public Class<? extends CdmValue> valueClass() {
        return valueClass;
    }

    /** True if {@code value} is either {@link CdmValue.Null} or an instance of this type's variant. */
    public boolean accepts(CdmValue value) {
        return value instanceof CdmValue.Null || valueClass.isInstance(value);
    }
}
