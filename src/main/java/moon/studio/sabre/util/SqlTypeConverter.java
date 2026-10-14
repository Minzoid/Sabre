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
import java.time.ZoneId;

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
            if (raw instanceof Timestamp ts) return ts;
            if (raw instanceof Number n) return new Timestamp(n.longValue());
            String str = raw.toString().trim();
            if (str.matches("^\\d+$")) return new Timestamp(Long.parseLong(str));
            try {
                return rs.getTimestamp(columnName);
            } catch (SQLException ignored) {
                return Timestamp.valueOf(str);
            }
        } else if (fieldType == java.sql.Date.class) {
            if (raw instanceof java.sql.Date d) return d;
            if (raw instanceof Number n) return new java.sql.Date(n.longValue());
            String str = raw.toString().trim();
            if (str.matches("^\\d+$")) return new java.sql.Date(Long.parseLong(str));
            try {
                return rs.getDate(columnName);
            } catch (SQLException ignored) {
                return java.sql.Date.valueOf(str);
            }
        } else if (fieldType == java.util.Date.class) {
            if (raw instanceof java.util.Date d) return d;
            if (raw instanceof Number n) return new java.util.Date(n.longValue());
            String str = raw.toString().trim();
            if (str.matches("^\\d+$")) return new java.util.Date(Long.parseLong(str));
            try {
                Timestamp ts = rs.getTimestamp(columnName);
                return ts != null ? new java.util.Date(ts.getTime()) : null;
            } catch (SQLException ignored) {
                return null;
            }
        } else if (fieldType == LocalDateTime.class) {
            if (raw instanceof LocalDateTime ldt) return ldt;
            if (raw instanceof Timestamp ts) return ts.toLocalDateTime();
            if (raw instanceof Number n) {
                return Instant.ofEpochMilli(n.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            String str = raw.toString().trim();
            if (str.matches("^\\d+$")) {
                return Instant.ofEpochMilli(Long.parseLong(str)).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            try {
                return Timestamp.valueOf(str).toLocalDateTime();
            } catch (IllegalArgumentException e) {
                if (str.contains(" ")) {
                    str = str.replace(" ", "T");
                }
                return LocalDateTime.parse(str);
            }
        } else if (fieldType == LocalDate.class) {
            if (raw instanceof LocalDate ld) return ld;
            if (raw instanceof java.sql.Date d) return d.toLocalDate();
            if (raw instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate();
            if (raw instanceof Number n) {
                return Instant.ofEpochMilli(n.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            String str = raw.toString().trim();
            if (str.matches("^\\d+$")) {
                return Instant.ofEpochMilli(Long.parseLong(str)).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (str.contains("T")) {
                str = str.split("T")[0];
            } else if (str.contains(" ")) {
                str = str.split(" ")[0];
            }
            return LocalDate.parse(str);
        } else if (fieldType == Instant.class) {
            if (raw instanceof Instant inst) return inst;
            if (raw instanceof Timestamp ts) return ts.toInstant();
            if (raw instanceof Number n) {
                return Instant.ofEpochMilli(n.longValue());
            }
            String str = raw.toString().trim();
            if (str.matches("^\\d+$")) {
                return Instant.ofEpochMilli(Long.parseLong(str));
            }
            try {
                return Timestamp.valueOf(str).toInstant();
            } catch (IllegalArgumentException e) {
                return Instant.parse(str);
            }
        } else if (fieldType == byte[].class) {
            return rs.getBytes(columnName);
        } else {
            return rs.getObject(columnName);
        }
    }
}
