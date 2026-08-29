package com.DBArena.engine.adapters.postgres;

import com.DBArena.common.core.value.CdmValue;
import com.DBArena.engine.spi.cdm.CdmType;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * {@link CdmValue} &lt;-&gt; JDBC, in both directions. {@code TIMESTAMP} is
 * always bound/read as {@link OffsetDateTime} pinned to UTC - never
 * {@code java.sql.Timestamp}, which carries an implicit JVM-local timezone
 * and is exactly what hard rule #9 forbids ("never with engine-local
 * timezone"). {@code DECIMAL} is always bound/read as {@link
 * java.math.BigDecimal} - never a {@code double} (also hard rule #9).
 */
final class CdmValueJdbcCodec {

    private CdmValueJdbcCodec() {
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
            case TIMESTAMP -> statement.setObject(index, toOffsetDateTime(((CdmValue.Timestamp) value).epochMillis()));
            case JSON -> statement.setObject(index, toJsonb(((CdmValue.Json) value).canonicalJson()));
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
                    resultSet.getObject(index, OffsetDateTime.class).toInstant().toEpochMilli());
            case JSON -> new CdmValue.Json(resultSet.getString(index));
        };
    }

    private static int jdbcSqlType(CdmType type) {
        return switch (type) {
            case BOOLEAN -> Types.BOOLEAN;
            case INTEGER -> Types.BIGINT;
            case DECIMAL -> Types.NUMERIC;
            case TEXT -> Types.VARCHAR;
            case TIMESTAMP -> Types.TIMESTAMP_WITH_TIMEZONE;
            case JSON -> Types.OTHER;
        };
    }

    private static OffsetDateTime toOffsetDateTime(long epochMillis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private static PGobject toJsonb(String canonicalJson) throws SQLException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(canonicalJson);
        return jsonb;
    }
}
