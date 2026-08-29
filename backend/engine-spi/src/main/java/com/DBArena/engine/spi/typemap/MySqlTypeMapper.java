package com.DBArena.engine.spi.typemap;

import com.DBArena.engine.spi.cdm.CdmType;

/**
 * The canonical {@code CdmType} -&gt; {@link MySqlColumnType} mapping. See
 * {@link MySqlColumnType} for the rationale behind each choice. Stateless
 * and thread-safe; adapter-mysql is expected to hold one shared instance,
 * not construct one per call - same posture as {@link PostgresTypeMapper}.
 * A total {@code switch} with no {@code default} branch, per
 * backend/CLAUDE.md's "Adding a new engine": a seventh {@code CdmType}
 * variant is a compiler error here until this mapper is updated too.
 */
public final class MySqlTypeMapper implements TypeMapper<MySqlColumnType> {

    @Override
    public MySqlColumnType map(CdmType cdmType) {
        return switch (cdmType) {
            case BOOLEAN -> MySqlColumnType.TINYINT_BOOL;
            case INTEGER -> MySqlColumnType.BIGINT;
            case DECIMAL -> MySqlColumnType.DECIMAL;
            case TEXT -> MySqlColumnType.TEXT;
            case TIMESTAMP -> MySqlColumnType.DATETIME;
            case JSON -> MySqlColumnType.JSON;
        };
    }
}
