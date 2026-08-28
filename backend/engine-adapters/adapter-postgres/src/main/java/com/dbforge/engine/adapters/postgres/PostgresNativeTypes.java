package com.dbforge.engine.adapters.postgres;

import com.dbforge.engine.spi.cdm.CdmType;

import java.util.Map;
import java.util.Optional;

/**
 * The reverse of {@link com.dbforge.engine.spi.typemap.PostgresTypeMapper}:
 * a Postgres native short type name (the pg_catalog/{@code udt_name} form -
 * {@code bool}/{@code int8}/{@code numeric}/{@code text}/{@code timestamptz}/{@code jsonb},
 * the same short names {@link java.sql.ResultSetMetaData#getColumnTypeName}
 * returns) back to the {@link CdmType} it materializes. Serves both
 * {@link PostgresIntrospector} (via {@code information_schema.columns.udt_name})
 * and {@link PostgresEngineAdapter#execute} (via result-set metadata) -
 * one table, one place these six strings are spelled.
 *
 * <p>Hand-maintained rather than derived from
 * {@code PostgresColumnType.sqlTypeName()}: two of those
 * ({@code boolean}/{@code bigint}) are the long DDL-form name, not the
 * short catalog name this class needs - see each entry below.
 */
final class PostgresNativeTypes {

    private PostgresNativeTypes() {
    }

    private static final Map<String, CdmType> BY_NATIVE_NAME = Map.of(
            "bool", CdmType.BOOLEAN,     // PostgresColumnType.BOOLEAN.sqlTypeName() is "boolean" (DDL form); pg_catalog's short name is "bool".
            "int8", CdmType.INTEGER,     // "bigint" (DDL form) vs "int8" (short/catalog form).
            "numeric", CdmType.DECIMAL,
            "text", CdmType.TEXT,
            "timestamptz", CdmType.TIMESTAMP,
            "jsonb", CdmType.JSON);

    /**
     * Empty for any native type outside the six the CDM type system
     * covers. {@link PostgresIntrospector} treats that as a bug (it only
     * ever introspects schemas this adapter itself created); {@link
     * PostgresEngineAdapter#execute} treats it as an arbitrary learner
     * query touching a Postgres feature outside the CDM and falls back to
     * a textual representation - see that method.
     */
    static Optional<CdmType> toCdmType(String nativeTypeName) {
        return Optional.ofNullable(BY_NATIVE_NAME.get(nativeTypeName));
    }
}
