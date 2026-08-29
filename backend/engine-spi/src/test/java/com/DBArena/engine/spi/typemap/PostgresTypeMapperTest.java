package com.DBArena.engine.spi.typemap;

import com.DBArena.engine.spi.cdm.CdmType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresTypeMapperTest {

    private final PostgresTypeMapper mapper = new PostgresTypeMapper();

    @Test
    void mapsEveryCdmTypeToItsDocumentedPostgresType() {
        assertThat(mapper.map(CdmType.BOOLEAN)).isEqualTo(PostgresColumnType.BOOLEAN);
        assertThat(mapper.map(CdmType.INTEGER)).isEqualTo(PostgresColumnType.BIGINT);
        assertThat(mapper.map(CdmType.DECIMAL)).isEqualTo(PostgresColumnType.NUMERIC);
        assertThat(mapper.map(CdmType.TEXT)).isEqualTo(PostgresColumnType.TEXT);
        assertThat(mapper.map(CdmType.TIMESTAMP)).isEqualTo(PostgresColumnType.TIMESTAMPTZ);
        assertThat(mapper.map(CdmType.JSON)).isEqualTo(PostgresColumnType.JSONB);
    }

    @ParameterizedTest
    @EnumSource(CdmType.class)
    void neverReturnsNullForAnyCdmType(CdmType type) {
        assertThat(mapper.map(type)).isNotNull();
    }

    @Test
    void everyPostgresColumnTypeExposesANonBlankSqlTypeName() {
        for (PostgresColumnType type : PostgresColumnType.values()) {
            assertThat(type.sqlTypeName()).isNotBlank();
        }
    }

    @Test
    void decimalMapsToUnboundedNumericNotAFixedPrecisionScale() {
        assertThat(mapper.map(CdmType.DECIMAL).sqlTypeName()).isEqualTo("numeric");
    }

    @Test
    void timestampMapsToTimestamptzNotAPlainTimestamp() {
        assertThat(mapper.map(CdmType.TIMESTAMP).sqlTypeName()).isEqualTo("timestamptz");
    }

    @Test
    void jsonMapsToJsonbNotPlainJson() {
        assertThat(mapper.map(CdmType.JSON).sqlTypeName()).isEqualTo("jsonb");
    }

    @Test
    void integerMapsToBigintNotANarrowerIntegerType() {
        assertThat(mapper.map(CdmType.INTEGER).sqlTypeName()).isEqualTo("bigint");
    }

    @Test
    void mappingIsPureEveryCallForTheSameTypeReturnsTheSameValue() {
        assertThat(mapper.map(CdmType.TEXT)).isSameAs(mapper.map(CdmType.TEXT));
    }

    @Test
    void everyCdmTypeMapsToADistinctPostgresColumnTypeNoTwoTypesCollapseTogether() {
        long distinctMappings = java.util.Arrays.stream(CdmType.values())
                .map(mapper::map)
                .distinct()
                .count();
        assertThat(distinctMappings).isEqualTo(CdmType.values().length);
    }
}
