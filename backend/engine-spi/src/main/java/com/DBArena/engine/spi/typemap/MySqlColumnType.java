package com.DBArena.engine.spi.typemap;

/**
 * A native MySQL column type used to materialize one
 * {@link com.DBArena.engine.spi.cdm.CdmColumn}. One entry per
 * {@link com.DBArena.engine.spi.cdm.CdmType} variant - see
 * {@link MySqlTypeMapper} for the mapping itself.
 */
public enum MySqlColumnType {

    /**
     * {@code CdmType.BOOLEAN} -&gt; {@code tinyint(1)}. MySQL has no native
     * boolean storage type - {@code BOOLEAN}/{@code BOOL} are parser-level
     * synonyms for {@code TINYINT(1)}, not a distinct type. Spelling the DDL
     * as {@code tinyint(1)} explicitly (rather than the {@code boolean}
     * alias) keeps the on-disk/catalog representation exactly what
     * introspection later reads back via {@code information_schema}, with
     * no alias-resolution step to trust.
     */
    TINYINT_BOOL("tinyint(1)"),

    /**
     * {@code CdmType.INTEGER} -&gt; {@code bigint}. {@code CdmValue.Int}
     * carries a {@code long}, so anything narrower risks silent overflow on
     * materialization - same reasoning as {@code PostgresColumnType.BIGINT}.
     */
    BIGINT("bigint"),

    /**
     * {@code CdmType.DECIMAL} -&gt; {@code decimal(65,30)} - MySQL's actual
     * maximum precision (65 total digits) and scale (30 fractional digits).
     * Unlike Postgres's {@code numeric}, MySQL's {@code DECIMAL} has no
     * unbounded form - a precision/scale pair is mandatory. The maximum is
     * chosen rather than a smaller fixed pair so the column accepts
     * whatever scale a given seed row or generated value actually uses (a
     * {@code CdmColumn} declares no precision/scale of its own -
     * {@code CdmValue.Decimal} carries its own scale per value, hard rule
     * #9) up to MySQL's own ceiling - the same open gap B03/B04 already
     * flagged for Postgres (no validator-level check against either
     * engine's real limit) applies here too.
     */
    DECIMAL("decimal(65,30)"),

    /**
     * {@code CdmType.TEXT} -&gt; {@code text}, never a bounded
     * {@code varchar(n)} - mirrors {@code PostgresColumnType.TEXT}. Always
     * paired with an explicit {@code utf8mb4}/{@code utf8mb4_bin} character
     * set and collation at the DDL level (hard rule #9's pinned MySQL
     * collation) - this enum only names the storage type, not the
     * collation clause; see {@code MySqlDdlBuilder}.
     */
    TEXT("text"),

    /**
     * {@code CdmType.TIMESTAMP} -&gt; {@code datetime(3)}, deliberately not
     * MySQL's {@code TIMESTAMP} type. MySQL {@code TIMESTAMP} converts
     * between the server/session {@code time_zone} and UTC on every
     * read/write and is range-limited to 1970-2038 - exactly the
     * "engine-local timezone" hard rule #9 forbids. {@code DATETIME} is a
     * naive wall-clock value with no implicit zone conversion at all, so
     * the adapter can bind/read it as a literal UTC value with zero
     * dependency on any session/driver timezone setting. The {@code (3)}
     * fractional-second precision matches {@code CdmValue.Timestamp}'s
     * millisecond-resolution {@code epochMillis}.
     */
    DATETIME("datetime(3)"),

    /**
     * {@code CdmType.JSON} -&gt; {@code json} (native MySQL JSON type, 5.7.8+).
     * Validates and normalizes on write, matching {@code CdmValue.Json}'s
     * canonical-text contract the same way Postgres's {@code jsonb} does.
     */
    JSON("json");

    private final String sqlTypeName;

    MySqlColumnType(String sqlTypeName) {
        this.sqlTypeName = sqlTypeName;
    }

    /** The literal SQL type name to use in a {@code CREATE TABLE} column definition. */
    public String sqlTypeName() {
        return sqlTypeName;
    }
}
