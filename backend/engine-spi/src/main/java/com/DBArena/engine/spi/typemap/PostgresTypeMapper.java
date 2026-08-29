package com.DBArena.engine.spi.typemap;

import com.DBArena.engine.spi.cdm.CdmType;

/**
 * The canonical {@code CdmType} -&gt; {@link PostgresColumnType} mapping. See
 * {@link PostgresColumnType} for the rationale behind each choice.
 * Stateless and thread-safe; adapter-postgres (B04) is expected to hold one
 * shared instance, not construct one per call.
 */
public final class PostgresTypeMapper implements TypeMapper<PostgresColumnType> {

    @Override
    public PostgresColumnType map(CdmType cdmType) {
        return switch (cdmType) {
            case BOOLEAN -> PostgresColumnType.BOOLEAN;
            case INTEGER -> PostgresColumnType.BIGINT;
            case DECIMAL -> PostgresColumnType.NUMERIC;
            case TEXT -> PostgresColumnType.TEXT;
            case TIMESTAMP -> PostgresColumnType.TIMESTAMPTZ;
            case JSON -> PostgresColumnType.JSONB;
        };
    }
}
