package com.DBArena.services.execution.evaluator;

import com.DBArena.common.core.value.CdmValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CdmValueStringifierTest {

    @Test
    void nullBecomesAJavaNullNotTheStringNull() {
        assertThat(CdmValueStringifier.toDisplayString(CdmValue.Null.INSTANCE)).isNull();
    }

    @Test
    void boolStringifiesAsTrueOrFalse() {
        assertThat(CdmValueStringifier.toDisplayString(new CdmValue.Bool(true))).isEqualTo("true");
    }

    @Test
    void decimalStringifiesAsPlainDecimalNotScientificNotation() {
        String result = CdmValueStringifier.toDisplayString(CdmValue.Decimal.of(new BigDecimal("19.99")));
        assertThat(result).isEqualTo("19.99");
    }

    @Test
    void timestampStringifiesAsIso8601Utc() {
        String result = CdmValueStringifier.toDisplayString(new CdmValue.Timestamp(0L));
        assertThat(result).isEqualTo("1970-01-01T00:00:00Z");
    }

    @Test
    void textPassesThroughUnchanged() {
        assertThat(CdmValueStringifier.toDisplayString(new CdmValue.Text("hello"))).isEqualTo("hello");
    }
}
