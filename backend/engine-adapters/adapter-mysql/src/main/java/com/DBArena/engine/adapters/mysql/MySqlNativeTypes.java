package com.DBArena.engine.adapters.mysql;

import com.DBArena.engine.spi.cdm.CdmType;

import java.util.Map;
import java.util.Optional;

/**
 * The reverse of {@link com.DBArena.engine.spi.typemap.MySqlTypeMapper}:
 * a MySQL native type name back to the {@link CdmType} it materializes.
 * Serves two different callers with two different native-name sources,
 * unlike Postgres's single-table {@code PostgresNativeTypes} - MySQL's
 * {@code TINYINT(1)} boolean encoding needs disambiguating from a plain
 * {@code TINYINT}, and the two sources this class reads from disagree on
 * whether that display width is even visible:
 *
 * <ul>
 *   <li>{@link #fromColumnType} - {@code information_schema.columns.COLUMN_TYPE},
 *   the full type spec including display width/precision (e.g.
 *   {@code "tinyint(1)"}, {@code "decimal(65,30)"}) exactly as this
 *   adapter's own DDL wrote it. Used by {@link MySqlIntrospector}, which -
 *   like {@code PostgresIntrospector} - only ever introspects schemas this
 *   adapter itself created, so an unrecognized {@code COLUMN_TYPE} there is
 *   a bug, not an arbitrary learner query.</li>
 *   <li>{@link #fromDriverType} - {@link java.sql.ResultSetMetaData#getColumnTypeName}
 *   plus {@link java.sql.ResultSetMetaData#getPrecision}, from an arbitrary
 *   executed statement's result set. Connector/J's driver-level type name
 *   does not include display width (a boolean {@code tinyint(1)} column and
 *   any other {@code tinyint} column both report {@code "TINYINT"}), so
 *   precision (which MySQL does surface as the column's display width for
 *   integer types) is the only signal available to tell them apart. Used by
 *   {@link MySqlEngineAdapter#execute}, which - like Postgres's equivalent -
 *   falls back to {@code CdmType.TEXT} for anything this doesn't resolve,
 *   since an arbitrary learner query can touch any MySQL feature outside
 *   the six the CDM models.</li>
 * </ul>
 */
final class MySqlNativeTypes {

    private MySqlNativeTypes() {
    }

    private static final Map<String, CdmType> BY_COLUMN_TYPE = Map.of(
            "tinyint(1)", CdmType.BOOLEAN,
            "bigint", CdmType.INTEGER,
            "decimal(65,30)", CdmType.DECIMAL,
            "text", CdmType.TEXT,
            "datetime(3)", CdmType.TIMESTAMP,
            "json", CdmType.JSON);

    /** See {@link #fromColumnType} - keyed on {@code information_schema.columns.COLUMN_TYPE}, lower-cased. */
    static Optional<CdmType> fromColumnType(String columnType) {
        return Optional.ofNullable(BY_COLUMN_TYPE.get(columnType));
    }

    /**
     * See {@link #fromDriverType} - keyed on {@link java.sql.ResultSetMetaData#getColumnTypeName}
     * (upper-cased by convention, matched case-insensitively here) plus
     * {@link java.sql.ResultSetMetaData#getPrecision} for the one type
     * (TINYINT) that needs it to disambiguate BOOLEAN from a narrower
     * integer column outside the CDM's own type system.
     */
    static Optional<CdmType> fromDriverType(String driverTypeName, int precision) {
        if (driverTypeName == null) {
            return Optional.empty();
        }
        return switch (driverTypeName.toUpperCase(java.util.Locale.ROOT)) {
            case "TINYINT" -> precision == 1 ? Optional.of(CdmType.BOOLEAN) : Optional.empty();
            case "BIGINT" -> Optional.of(CdmType.INTEGER);
            case "DECIMAL", "NEWDECIMAL" -> Optional.of(CdmType.DECIMAL);
            case "TEXT" -> Optional.of(CdmType.TEXT);
            case "DATETIME" -> Optional.of(CdmType.TIMESTAMP);
            case "JSON" -> Optional.of(CdmType.JSON);
            default -> Optional.empty();
        };
    }
}
