package com.DBArena.engine.adapters.mysql;

import com.DBArena.common.core.value.CdmValue;
import com.DBArena.engine.spi.cdm.CdmType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * {@link CdmValue} &lt;-&gt; JDBC, in both directions - mirrors {@code
 * CdmValueJdbcCodec} (adapter-postgres) with one deliberate difference:
 * {@code TIMESTAMP} is bound/read as a zone-naive {@link LocalDateTime}
 * against a {@code DATETIME} column, not {@link java.time.OffsetDateTime}
 * against a zone-aware type - see {@code MySqlColumnType.DATETIME}'s
 * Javadoc for why MySQL's own {@code TIMESTAMP} type is unsuitable here.
 * The {@link LocalDateTime} is always constructed/interpreted against
 * {@link ZoneOffset#UTC} by this class - never the JVM or MySQL session
 * default zone - which is what actually satisfies hard rule #9, not the
 * column type choice alone. {@code DECIMAL} is always bound/read as
 * {@link java.math.BigDecimal} - never a {@code double} (hard rule #9,
 * same as Postgres). {@code JSON} is bound/read as a plain {@link String}
 * - unlike Postgres's {@code jsonb} (which needs a {@code PGobject}
 * wrapper to select the target type), MySQL's JSON columns accept and
 * return ordinary text over the wire, so no engine-specific wrapper type
 * is needed here at all.
 */
final class MySqlValueJdbcCodec {

    private MySqlValueJdbcCodec() {
    }

    static void bind(PreparedStatement statement, int index, CdmType type, CdmValue value) throws SQLException {
        if (value instanceof CdmValue.Null) {
            statement.setNull(index, jdbcSqlType(type));
            return;
        }
        switch (type) {
            case BOOLEAN -> statement.setBoolean(index, ((CdmValue.Bool) value).value());
            case INTEGER -> statement.setLong(index, ((CdmValue.Int) value).value());
            case DECIMAL -> statement.setBigDecimal(index, ((CdmValue.Decimal) value).toBigDecimal());
            case TEXT -> statement.setString(index, ((CdmValue.Text) value).value());
            case TIMESTAMP -> statement.setObject(index, toLocalDateTime(((CdmValue.Timestamp) value).epochMillis()));
            case JSON -> statement.setString(index, ((CdmValue.Json) value).canonicalJson());
        }
    }

    /** Reads column {@code index} of the current row as a {@link CdmValue} of the given {@code type}. */
    static CdmValue read(ResultSet resultSet, int index, CdmType type) throws SQLException {
        // getObject() first, purely to detect SQL NULL without a type-specific getter's
        // ambiguous zero-value-vs-null return (e.g. getLong() returning 0 for both).
        if (resultSet.getObject(index) == null) {
            return CdmValue.Null.INSTANCE;
        }
        return switch (type) {
            case BOOLEAN -> new CdmValue.Bool(resultSet.getBoolean(index));
            case INTEGER -> new CdmValue.Int(resultSet.getLong(index));
            case DECIMAL -> CdmValue.Decimal.of(resultSet.getBigDecimal(index));
            case TEXT -> new CdmValue.Text(resultSet.getString(index));
            case TIMESTAMP -> new CdmValue.Timestamp(
                    resultSet.getObject(index, LocalDateTime.class).toInstant(ZoneOffset.UTC).toEpochMilli());
            case JSON -> new CdmValue.Json(resultSet.getString(index));
        };
    }

    private static int jdbcSqlType(CdmType type) {
        return switch (type) {
            case BOOLEAN -> Types.TINYINT;
            case INTEGER -> Types.BIGINT;
            case DECIMAL -> Types.DECIMAL;
            case TEXT -> Types.VARCHAR;
            case TIMESTAMP -> Types.TIMESTAMP;
            case JSON -> Types.VARCHAR;
        };
    }

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }
}
