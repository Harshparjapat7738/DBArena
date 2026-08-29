package com.DBArena.services.execution.evaluator;

import com.DBArena.common.core.value.CdmValue;

import java.time.Instant;

/** Display-only stringification (never fed back into a comparison - hard rule #9 governs comparison, not display). {@code null} (the Java reference, not the string) for {@link CdmValue.Null}, preserving JSON {@code null} on the wire. */
public final class CdmValueStringifier {

    private CdmValueStringifier() {
    }

    public static String toDisplayString(CdmValue value) {
        return switch (value) {
            case CdmValue.Null ignored -> null;
            case CdmValue.Bool bool -> String.valueOf(bool.value());
            case CdmValue.Int i -> String.valueOf(i.value());
            case CdmValue.Decimal decimal -> decimal.toBigDecimal().toPlainString();
            case CdmValue.Text text -> text.value();
            case CdmValue.Timestamp timestamp -> Instant.ofEpochMilli(timestamp.epochMillis()).toString();
            case CdmValue.Json json -> json.canonicalJson();
        };
    }
}
