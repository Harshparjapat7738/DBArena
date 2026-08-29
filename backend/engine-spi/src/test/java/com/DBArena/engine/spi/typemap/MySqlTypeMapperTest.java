package com.DBArena.engine.spi.typemap;

import com.DBArena.engine.spi.cdm.CdmType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlTypeMapperTest {

    private final MySqlTypeMapper mapper = new MySqlTypeMapper();

    @Test
    void mapsEveryCdmTypeToItsDocumentedMySqlType() {
        assertThat(mapper.map(CdmType.BOOLEAN)).isEqualTo(MySqlColumnType.TINYINT_BOOL);
        assertThat(mapper.map(CdmType.INTEGER)).isEqualTo(MySqlColumnType.BIGINT);
        assertThat(mapper.map(CdmType.DECIMAL)).isEqualTo(MySqlColumnType.DECIMAL);
        assertThat(mapper.map(CdmType.TEXT)).isEqualTo(MySqlColumnType.TEXT);
        assertThat(mapper.map(CdmType.TIMESTAMP)).isEqualTo(MySqlColumnType.DATETIME);
        assertThat(mapper.map(CdmType.JSON)).isEqualTo(MySqlColumnType.JSON);
    }

    @ParameterizedTest
    @EnumSource(CdmType.class)
    void neverReturnsNullForAnyCdmType(CdmType type) {
        assertThat(mapper.map(type)).isNotNull();
    }

    @Test
    void everyMySqlColumnTypeExposesANonBlankSqlTypeName() {
        for (MySqlColumnType type : MySqlColumnType.values()) {
            assertThat(type.sqlTypeName()).isNotBlank();
        }
    }

    @Test
    void booleanMapsToTinyintOneNotTheBooleanAlias() {
        assertThat(mapper.map(CdmType.BOOLEAN).sqlTypeName()).isEqualTo("tinyint(1)");
    }

    @Test
    void decimalMapsToItsMaximumPrecisionAndScaleNotAnUnboundedType() {
        assertThat(mapper.map(CdmType.DECIMAL).sqlTypeName()).isEqualTo("decimal(65,30)");
    }

    @Test
    void timestampMapsToDatetimeNotMySqlsTimestampType() {
        assertThat(mapper.map(CdmType.TIMESTAMP).sqlTypeName()).isEqualTo("datetime(3)");
    }

    @Test
    void jsonMapsToTheNativeJsonType() {
        assertThat(mapper.map(CdmType.JSON).sqlTypeName()).isEqualTo("json");
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
    void everyCdmTypeMapsToADistinctMySqlColumnTypeNoTwoTypesCollapseTogether() {
        long distinctMappings = java.util.Arrays.stream(CdmType.values())
                .map(mapper::map)
                .distinct()
                .count();
        assertThat(distinctMappings).isEqualTo(CdmType.values().length);
    }
}
