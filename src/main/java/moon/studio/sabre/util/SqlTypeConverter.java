package moon.studio.sabre.util;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class SqlTypeConverter {

    private SqlTypeConverter() {}

    public static void setParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else if (value instanceof String s) {
            stmt.setString(index, s);
        } else if (value instanceof Integer i) {
            stmt.setInt(index, i);
        } else if (value instanceof Long l) {
            stmt.setLong(index, l);
        } else if (value instanceof Double d) {
            stmt.setDouble(index, d);
        } else if (value instanceof Float f) {
            stmt.setFloat(index, f);
        } else if (value instanceof Boolean b) {
            stmt.setBoolean(index, b);
        } else if (value instanceof Short s) {
            stmt.setShort(index, s);
        } else if (value instanceof Byte b) {
            stmt.setByte(index, b);
        } else if (value instanceof BigDecimal bd) {
            stmt.setBigDecimal(index, bd);
        } else if (value instanceof Timestamp ts) {
            stmt.setTimestamp(index, ts);
        } else if (value instanceof java.sql.Date d) {
            stmt.setDate(index, d);
        } else if (value instanceof java.util.Date d) {
            stmt.setTimestamp(index, new Timestamp(d.getTime()));
        } else if (value instanceof LocalDateTime ldt) {
            stmt.setTimestamp(index, Timestamp.valueOf(ldt));
        } else if (value instanceof LocalDate ld) {
            stmt.setDate(index, java.sql.Date.valueOf(ld));
        } else if (value instanceof Instant instant) {
            stmt.setTimestamp(index, Timestamp.from(instant));
        } else if (value instanceof byte[] bytes) {
            stmt.setBytes(index, bytes);
        } else {
            stmt.setObject(index, value);
        }
    }

    public static Object getColumnValue(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        Object raw = rs.getObject(columnName);
        if (raw == null || rs.wasNull()) {
            return null;
        }

        if (fieldType == int.class || fieldType == Integer.class) {
            return rs.getInt(columnName);
        } else if (fieldType == long.class || fieldType == Long.class) {
            return rs.getLong(columnName);
        } else if (fieldType == double.class || fieldType == Double.class) {
            return rs.getDouble(columnName);
        } else if (fieldType == float.class || fieldType == Float.class) {
            return rs.getFloat(columnName);
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return rs.getBoolean(columnName);
        } else if (fieldType == short.class || fieldType == Short.class) {
            return rs.getShort(columnName);
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            return rs.getByte(columnName);
        } else if (fieldType == String.class) {
            return rs.getString(columnName);
        } else if (fieldType == BigDecimal.class) {
            return rs.getBigDecimal(columnName);
        } else if (fieldType == Timestamp.class) {
            return rs.getTimestamp(columnName);
        } else if (fieldType == java.sql.Date.class) {
            return rs.getDate(columnName);
        } else if (fieldType == java.util.Date.class) {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts != null ? new java.util.Date(ts.getTime()) : null;
        } else if (fieldType == LocalDateTime.class) {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts != null ? ts.toLocalDateTime() : null;
        } else if (fieldType == LocalDate.class) {
            java.sql.Date d = rs.getDate(columnName);
            return d != null ? d.toLocalDate() : null;
        } else if (fieldType == Instant.class) {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts != null ? ts.toInstant() : null;
        } else if (fieldType == byte[].class) {
            return rs.getBytes(columnName);
        } else {
            return rs.getObject(columnName);
        }
    }
}
