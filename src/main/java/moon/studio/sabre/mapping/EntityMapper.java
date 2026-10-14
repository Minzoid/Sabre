package moon.studio.sabre.mapping;

import moon.studio.sabre.core.SabreException;
import moon.studio.sabre.mapping.annotations.Column;
import moon.studio.sabre.mapping.annotations.Id;
import moon.studio.sabre.mapping.annotations.Table;
import moon.studio.sabre.mapping.annotations.Transient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityMapper {

    private static final ConcurrentHashMap<Class<?>, EntityMeta> CACHE = new ConcurrentHashMap<>();

    private EntityMapper() {}

    public record EntityMeta(
            String tableName,
            Field idField,
            String idColumnName,
            Map<String, Field> columnToField
    ) {
        public Map<String, Field> nonIdColumns() {
            Map<String, Field> result = new LinkedHashMap<>();
            for (Map.Entry<String, Field> entry : columnToField.entrySet()) {
                if (!entry.getKey().equals(idColumnName)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
            return result;
        }
    }

    public static <T> EntityMeta getMeta(Class<T> type) {
        return CACHE.computeIfAbsent(type, EntityMapper::resolve);
    }

    public static Object getIdValue(Object entity) {
        EntityMeta meta = getMeta(entity.getClass());
        try {
            meta.idField().setAccessible(true);
            return meta.idField().get(entity);
        } catch (IllegalAccessException e) {
            throw new SabreException("Cannot read @Id field value", e);
        }
    }

    private static EntityMeta resolve(Class<?> type) {
        Table tableAnnotation = type.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new SabreException("Class " + type.getName() + " is not annotated with @Table");
        }
        String tableName = tableAnnotation.value();

        Field idField = null;
        String idColumnName = null;
        Map<String, Field> columnToField = new LinkedHashMap<>();

        // Walk class hierarchy up to Object to support inherited fields
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (field.isAnnotationPresent(Transient.class)) continue;

                field.setAccessible(true);

                Column colAnnotation = field.getAnnotation(Column.class);
                String columnName = (colAnnotation != null) ? colAnnotation.value() : field.getName();

                columnToField.putIfAbsent(columnName, field);

                if (field.isAnnotationPresent(Id.class)) {
                    if (idField != null && !idField.equals(field)) {
                        throw new SabreException("Class " + type.getName() + " has multiple @Id fields");
                    }
                    idField = field;
                    idColumnName = columnName;
                }
            }
            current = current.getSuperclass();
        }

        if (idField == null) {
            throw new SabreException("Class " + type.getName() + " has no field annotated with @Id");
        }

        return new EntityMeta(tableName, idField, idColumnName, columnToField);
    }
}
