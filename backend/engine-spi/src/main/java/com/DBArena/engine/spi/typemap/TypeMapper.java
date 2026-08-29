package com.DBArena.engine.spi.typemap;

import com.DBArena.engine.spi.cdm.CdmType;

/**
 * Maps every {@link CdmType} variant to one engine's native column/field
 * type. This is the "total {@code TypeMapper}" backend/CLAUDE.md's "Adding a
 * new engine" section requires of every engine: each implementation is
 * expected to be a {@code switch} over {@code CdmType} with no
 * {@code default} branch, so the compiler - not a runtime check - refuses to
 * build the moment a new {@code CdmType} variant is added until every
 * engine's mapper is updated to handle it. There is deliberately no
 * "unsupported type" exception path in this contract; if one is ever needed,
 * that is a sign {@code CdmType} grew a variant a given engine genuinely
 * cannot represent, which is a design conversation, not a mapping detail.
 *
 * @param <T> the engine-native type this mapper produces (e.g.
 *            {@link PostgresColumnType}, {@link MongoBsonType})
 */
public interface TypeMapper<T> {

    T map(CdmType cdmType);
}
