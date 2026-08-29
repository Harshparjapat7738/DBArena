package com.DBArena.engine.spi.typemap;

import com.DBArena.engine.spi.cdm.CdmType;

/**
 * The canonical {@code CdmType} -&gt; {@link MongoBsonType} mapping. See
 * {@link MongoBsonType} for the rationale behind each choice. Stateless and
 * thread-safe; adapter-mongodb (B05) is expected to hold one shared
 * instance, not construct one per call.
 */
public final class MongoTypeMapper implements TypeMapper<MongoBsonType> {

    @Override
    public MongoBsonType map(CdmType cdmType) {
        return switch (cdmType) {
            case BOOLEAN -> MongoBsonType.BOOLEAN;
            case INTEGER -> MongoBsonType.INT64;
            case DECIMAL -> MongoBsonType.DECIMAL128;
            case TEXT -> MongoBsonType.STRING;
            case TIMESTAMP -> MongoBsonType.INT64_EPOCH_MILLIS;
            case JSON -> MongoBsonType.DOCUMENT;
        };
    }
}
