package com.DBArena.engine.spi.typemap;

/**
 * A native PostgreSQL column type used to materialize one
 * {@link com.DBArena.engine.spi.cdm.CdmColumn}. One entry per
 * {@link com.DBArena.engine.spi.cdm.CdmType} variant - see
 * {@link PostgresTypeMapper} for the mapping itself.
 */
public enum PostgresColumnType {

    /** {@code CdmType.BOOLEAN} -&gt; {@code boolean}. */
    BOOLEAN("boolean"),

    /**
     * {@code CdmType.INTEGER} -&gt; {@code bigint}. {@code CdmValue.Int} carries
     * a {@code long}, so anything narrower (e.g. {@code integer}) would risk
     * silent overflow on materialization.
     */
    BIGINT("bigint"),

    /**
     * {@code CdmType.DECIMAL} -&gt; {@code numeric} (unbounded precision/scale,
     * not {@code numeric(p,s)}). A {@code CdmColumn} declares no fixed
     * precision or scale of its own - {@code CdmValue.Decimal} carries its own
     * scale per value (hard rule #9) - so the column must accept whatever
     * scale a given seed row or generated value actually uses, not a scale
     * fixed in advance.
     */
    NUMERIC("numeric"),

    /** {@code CdmType.TEXT} -&gt; {@code text}, never a bounded {@code varchar(n)}. */
    TEXT("text"),

    /**
     * {@code CdmType.TIMESTAMP} -&gt; {@code timestamptz}. Postgres always
     * normalizes and stores this internally as UTC regardless of session
     * timezone, so every read is convertible to the epoch-millis-UTC
     * representation hard rule #9 requires for comparison - the JDBC adapter
     * (B04) does that conversion at the boundary; this mapping only picks the
     * storage type.
     */
    TIMESTAMPTZ("timestamptz"),

    /**
     * {@code CdmType.JSON} -&gt; {@code jsonb} (binary, canonicalized on write),
     * not {@code json} (stores raw text verbatim). This matches
     * {@code CdmValue.Json}'s own canonical-text contract instead of
     * preserving arbitrary source formatting Postgres would otherwise keep.
     */
    JSONB("jsonb");

    private final String sqlTypeName;

    PostgresColumnType(String sqlTypeName) {
        this.sqlTypeName = sqlTypeName;
    }

    /** The literal SQL type name to use in a {@code CREATE TABLE} column definition. */
    public String sqlTypeName() {
        return sqlTypeName;
    }
}
