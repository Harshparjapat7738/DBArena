package com.dbforge.engine.spi.typemap;

/**
 * A native BSON representation used to shape one
 * {@link com.dbforge.engine.spi.cdm.CdmColumn}'s value when materializing a
 * document. One entry per {@link com.dbforge.engine.spi.cdm.CdmType}
 * variant - see {@link MongoTypeMapper} for the mapping itself.
 */
public enum MongoBsonType {

    /** {@code CdmType.BOOLEAN} -&gt; BSON {@code bool}. */
    BOOLEAN,

    /** {@code CdmType.INTEGER} -&gt; BSON {@code int64}. {@code CdmValue.Int} carries a {@code long}. */
    INT64,

    /**
     * {@code CdmType.DECIMAL} -&gt; BSON {@code decimal128}, never {@code double}
     * - the one BSON numeric representation that stores an exact scaled
     * value rather than a binary floating-point approximation, matching hard
     * rule #9's "never as doubles" for decimals.
     */
    DECIMAL128,

    /** {@code CdmType.TEXT} -&gt; BSON {@code string}. */
    STRING,

    /**
     * {@code CdmType.TIMESTAMP} -&gt; BSON {@code int64} epoch millis,
     * deliberately <b>not</b> the BSON {@code date} type. On the wire a BSON
     * date already is just an int64 epoch-millis value, but most driver and
     * tooling layers decode it straight into a language-local date/time
     * object - reintroducing exactly the "engine-local timezone" risk hard
     * rule #9 forbids. Storing a plain {@code int64} keeps the on-disk
     * representation byte-identical to the platform's own canonical
     * epoch-millis timestamp with no implicit conversion step anywhere in
     * the read path. catalog-service (M13) already established this same
     * convention for its own documents - this makes it the mapper's rule,
     * not an ad hoc choice repeated per service.
     */
    INT64_EPOCH_MILLIS,

    /**
     * {@code CdmType.JSON} -&gt; an embedded BSON {@code document} (or array),
     * parsed from {@code CdmValue.Json}'s canonical text - not stored as a
     * {@code string} field, so a learner can query into it with ordinary
     * Mongo dot-path queries instead of having to parse a JSON string
     * themselves.
     */
    DOCUMENT
}
