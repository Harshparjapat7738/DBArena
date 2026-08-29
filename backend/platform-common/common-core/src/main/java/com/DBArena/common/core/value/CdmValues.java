package com.DBArena.common.core.value;

import java.util.Comparator;

/**
 * Canonical equality and ordering for {@link CdmValue}. This is the one
 * place cross-engine value comparison happens - the result comparator
 * (B10) and the dataset cross-engine equivalence proof (B06) must both
 * route through here rather than reimplementing scale-aware decimal
 * comparison or timestamp comparison themselves.
 */
public final class CdmValues {

    private CdmValues() {
    }

    /**
     * True if two values are the <em>same value</em>, independent of
     * incidental representation: {@code Decimal(150, 2)} (1.50) equals
     * {@code Decimal(15, 1)} (1.5); two {@code Timestamp}s are equal iff
     * their epoch-millis are equal.
     */
    public static boolean equalCanonical(CdmValue a, CdmValue b) {
        if (a instanceof CdmValue.Null && b instanceof CdmValue.Null) {
            return true;
        }
        if (a instanceof CdmValue.Bool ba && b instanceof CdmValue.Bool bb) {
            return ba.value() == bb.value();
        }
        if (a instanceof CdmValue.Int ia && b instanceof CdmValue.Int ib) {
            return ia.value() == ib.value();
        }
        if (a instanceof CdmValue.Decimal da && b instanceof CdmValue.Decimal db) {
            return da.toBigDecimal().compareTo(db.toBigDecimal()) == 0;
        }
        if (a instanceof CdmValue.Text ta && b instanceof CdmValue.Text tb) {
            return ta.value().equals(tb.value());
        }
        if (a instanceof CdmValue.Timestamp tsa && b instanceof CdmValue.Timestamp tsb) {
            return tsa.epochMillis() == tsb.epochMillis();
        }
        if (a instanceof CdmValue.Json ja && b instanceof CdmValue.Json jb) {
            return ja.canonicalJson().equals(jb.canonicalJson());
        }
        return false;
    }

    /**
     * Total order over same-variant values, for sorting result rows before
     * comparison. Comparing across variants (e.g. {@code Int} vs {@code Text})
     * throws - the caller has a type-mapping bug if that happens, and
     * silently ordering by variant name would hide it.
     */
    public static int compareCanonical(CdmValue a, CdmValue b) {
        if (a instanceof CdmValue.Null && b instanceof CdmValue.Null) {
            return 0;
        }
        if (a instanceof CdmValue.Bool ba && b instanceof CdmValue.Bool bb) {
            return Boolean.compare(ba.value(), bb.value());
        }
        if (a instanceof CdmValue.Int ia && b instanceof CdmValue.Int ib) {
            return Long.compare(ia.value(), ib.value());
        }
        if (a instanceof CdmValue.Decimal da && b instanceof CdmValue.Decimal db) {
            return da.toBigDecimal().compareTo(db.toBigDecimal());
        }
        if (a instanceof CdmValue.Text ta && b instanceof CdmValue.Text tb) {
            return ta.value().compareTo(tb.value());
        }
        if (a instanceof CdmValue.Timestamp tsa && b instanceof CdmValue.Timestamp tsb) {
            return Long.compare(tsa.epochMillis(), tsb.epochMillis());
        }
        if (a instanceof CdmValue.Json) {
            throw new IllegalArgumentException("CdmValue.Json has no canonical order");
        }
        throw new IllegalArgumentException(
                "Cannot compare different CdmValue variants: " + a.getClass().getSimpleName()
                        + " vs " + b.getClass().getSimpleName());
    }

    public static Comparator<CdmValue> comparator() {
        return CdmValues::compareCanonical;
    }
}
