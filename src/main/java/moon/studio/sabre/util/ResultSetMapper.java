package moon.studio.sabre.util;

import moon.studio.sabre.core.SabreException;
import moon.studio.sabre.mapping.EntityMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ResultSetMapper {

    private ResultSetMapper() {}

    public static <T> List<T> mapAll(ResultSet rs, Class<T> type) {
        try {
            List<T> results = new ArrayList<>();
            EntityMapper.EntityMeta meta = EntityMapper.getMeta(type);
            ResultSetMetaData metaData = rs.getMetaData();

            // Lowercase lookup handles case variations returned across different JDBC drivers
            List<String> availableColumns = new ArrayList<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                availableColumns.add(metaData.getColumnLabel(i).toLowerCase());
            }

            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);

            while (rs.next()) {
                T instance = constructor.newInstance();

                for (Map.Entry<String, Field> entry : meta.columnToField().entrySet()) {
                    String columnName = entry.getKey();
                    Field field = entry.getValue();

                    if (!availableColumns.contains(columnName.toLowerCase())) continue;

                    Object value = SqlTypeConverter.getColumnValue(rs, columnName, field.getType());
                    if (value != null || !field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        field.set(instance, value);
                    }
                }

                results.add(instance);
            }

            return results;
        } catch (SQLException e) {
            throw new SabreException("Failed to map ResultSet", e);
        } catch (ReflectiveOperationException e) {
            throw new SabreException("Failed to instantiate or populate entity of type " + type.getName(), e);
        }
    }

    public static <T> T mapOne(ResultSet rs, Class<T> type) {
        List<T> results = mapAll(rs, type);
        return results.isEmpty() ? null : results.get(0);
    }
}
