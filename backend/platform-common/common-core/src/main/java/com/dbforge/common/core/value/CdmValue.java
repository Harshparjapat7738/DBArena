package com.dbforge.common.core.value;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The canonical scalar value model shared by the Canonical Dataset Model
 * (authored-once datasets, see docs/02 - not yet written, ask before
 * assuming its exact shape) and, later, the result comparator (B10). Every
 * engine adapter must translate its native driver types into these
 * variants and back - nothing downstream of materialization should ever
 * see a Postgres {@code java.sql.Timestamp} or a Mongo {@code BsonDecimal128}
 * directly.
 *
 * <p>This directly encodes hard rule #9 from CLAUDE.md: decimals are
 * represented as a scaled integer (never a {@code double}), timestamps as
 * a UTC epoch-millis {@code long} (never engine-local, never a floating
 * point). Two {@code CdmValue}s are only ever compared through
 * {@link CdmValues#equalCanonical}, not {@code Object.equals}, because a
 * naive record {@code equals} on {@link Decimal} would treat {@code 1.5}
 * and {@code 1.50} as different values when the comparator must treat
 * them as equal.
 */
public sealed interface CdmValue {

    record Null() implements CdmValue {
        public static final Null INSTANCE = new Null();
    }

    record Bool(boolean value) implements CdmValue {
    }

    record Int(long value) implements CdmValue {
    }

    /**
     * {@code unscaledValue * 10^-scale}, e.g. unscaled=150, scale=2 -> 1.50.
     * Mirrors {@link java.math.BigDecimal}'s own representation deliberately,
     * but as a record so it participates in the sealed CdmValue hierarchy.
     */
    record Decimal(BigInteger unscaledValue, int scale) implements CdmValue {

        public Decimal {
            if (unscaledValue == null) {
                throw new IllegalArgumentException("unscaledValue must not be null");
            }
            if (scale < 0) {
                throw new IllegalArgumentException("scale must be >= 0, got " + scale);
            }
        }

        public static Decimal of(BigDecimal value) {
            BigDecimal normalized = value.stripTrailingZeros();
            int scale = Math.max(normalized.scale(), 0);
            return new Decimal(normalized.setScale(scale).unscaledValue(), scale);
        }

        public BigDecimal toBigDecimal() {
            return new BigDecimal(unscaledValue, scale);
        }
    }

    record Text(String value) implements CdmValue {
        public Text {
            if (value == null) {
                throw new IllegalArgumentException("value must not be null");
            }
        }
    }

    /** UTC epoch milliseconds. Never engine-local, never a floating point. */
    record Timestamp(long epochMillis) implements CdmValue {
    }

    /** An opaque JSON/document fragment, for Mongo-shaped nested data. Stored as canonicalized text. */
    record Json(String canonicalJson) implements CdmValue {
        public Json {
            if (canonicalJson == null) {
                throw new IllegalArgumentException("canonicalJson must not be null");
            }
        }
    }
}
