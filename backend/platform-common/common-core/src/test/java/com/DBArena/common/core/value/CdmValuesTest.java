package com.DBArena.common.core.value;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CdmValuesTest {

    @Test
    void decimalsWithDifferentScaleButSameValueAreCanonicallyEqual() {
        CdmValue.Decimal oneFifty = new CdmValue.Decimal(BigInteger.valueOf(150), 2); // 1.50
        CdmValue.Decimal onePointFive = new CdmValue.Decimal(BigInteger.valueOf(15), 1); // 1.5

        assertThat(CdmValues.equalCanonical(oneFifty, onePointFive)).isTrue();
        assertThat(oneFifty).isNotEqualTo(onePointFive); // record equals must NOT be used for this
    }

    @Test
    void decimalOfNormalizesFromBigDecimal() {
        CdmValue.Decimal d = CdmValue.Decimal.of(new BigDecimal("3.140"));
        assertThat(d.toBigDecimal()).isEqualByComparingTo("3.14");
    }

    @Test
    void timestampsCompareOnEpochMillisOnly() {
        CdmValue.Timestamp a = new CdmValue.Timestamp(1_700_000_000_000L);
        CdmValue.Timestamp b = new CdmValue.Timestamp(1_700_000_000_000L);
        CdmValue.Timestamp c = new CdmValue.Timestamp(1_700_000_000_001L);

        assertThat(CdmValues.equalCanonical(a, b)).isTrue();
        assertThat(CdmValues.equalCanonical(a, c)).isFalse();
        assertThat(CdmValues.compareCanonical(a, c)).isLessThan(0);
    }

    @Test
    void nullEqualsNull() {
        assertThat(CdmValues.equalCanonical(CdmValue.Null.INSTANCE, new CdmValue.Null())).isTrue();
    }

    @Test
    void differentVariantsAreNeverCanonicallyEqual() {
        assertThat(CdmValues.equalCanonical(new CdmValue.Int(1), new CdmValue.Text("1"))).isFalse();
    }

    @Test
    void comparingDifferentVariantsThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> CdmValues.compareCanonical(new CdmValue.Int(1), new CdmValue.Text("1")));
    }
}
