package com.DBArena.engine.spi.typemap;

import com.DBArena.engine.spi.cdm.CdmType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class MongoTypeMapperTest {

    private final MongoTypeMapper mapper = new MongoTypeMapper();

    @Test
    void mapsEveryCdmTypeToItsDocumentedBsonType() {
        assertThat(mapper.map(CdmType.BOOLEAN)).isEqualTo(MongoBsonType.BOOLEAN);
        assertThat(mapper.map(CdmType.INTEGER)).isEqualTo(MongoBsonType.INT64);
        assertThat(mapper.map(CdmType.DECIMAL)).isEqualTo(MongoBsonType.DECIMAL128);
        assertThat(mapper.map(CdmType.TEXT)).isEqualTo(MongoBsonType.STRING);
        assertThat(mapper.map(CdmType.TIMESTAMP)).isEqualTo(MongoBsonType.INT64_EPOCH_MILLIS);
        assertThat(mapper.map(CdmType.JSON)).isEqualTo(MongoBsonType.DOCUMENT);
    }

    @ParameterizedTest
    @EnumSource(CdmType.class)
    void neverReturnsNullForAnyCdmType(CdmType type) {
        assertThat(mapper.map(type)).isNotNull();
    }

    @Test
    void decimalMapsToDecimal128NeverToADouble() {
        assertThat(mapper.map(CdmType.DECIMAL)).isEqualTo(MongoBsonType.DECIMAL128);
        assertThat(mapper.map(CdmType.DECIMAL)).isNotEqualTo(MongoBsonType.STRING);
    }

    @Test
    void timestampMapsToInt64EpochMillisNotTheBsonDateType() {
        // There is deliberately no MongoBsonType.DATE variant at all - see
        // MongoBsonType's Javadoc for why. This assertion is really about
        // MongoBsonType's shape, but asserting it here too keeps the
        // decision pinned at the one call site that would regress silently
        // if someone "simplified" this mapping back to a BSON date.
        assertThat(mapper.map(CdmType.TIMESTAMP)).isEqualTo(MongoBsonType.INT64_EPOCH_MILLIS);
    }

    @Test
    void jsonMapsToAnEmbeddedDocumentNotAStringField() {
        assertThat(mapper.map(CdmType.JSON)).isEqualTo(MongoBsonType.DOCUMENT);
    }

    @Test
    void integerAndTimestampAreBothPhysicallyInt64ButAreDistinctEnumValues() {
        // Same wire representation, different semantic meaning - callers
        // that need to know "is this field a timestamp" must be able to
        // branch on the mapping result without re-consulting CdmType.
        assertThat(mapper.map(CdmType.INTEGER)).isNotEqualTo(mapper.map(CdmType.TIMESTAMP));
    }

    @Test
    void mappingIsPureEveryCallForTheSameTypeReturnsTheSameValue() {
        assertThat(mapper.map(CdmType.TEXT)).isSameAs(mapper.map(CdmType.TEXT));
    }

    @Test
    void everyCdmTypeMapsToADistinctBsonTypeNoTwoTypesCollapseTogether() {
        long distinctMappings = java.util.Arrays.stream(CdmType.values())
                .map(mapper::map)
                .distinct()
                .count();
        assertThat(distinctMappings).isEqualTo(CdmType.values().length);
    }
}
