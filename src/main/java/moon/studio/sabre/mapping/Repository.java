package moon.studio.sabre.mapping;

import moon.studio.sabre.core.ConnectionManager;
import moon.studio.sabre.core.SabreException;
import moon.studio.sabre.util.ResultSetMapper;
import moon.studio.sabre.util.SqlTypeConverter;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Repository {

    private final ConnectionManager connectionManager;

    public Repository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void save(Object entity) {
        EntityMapper.EntityMeta meta = EntityMapper.getMeta(entity.getClass());
        Object currentId = EntityMapper.getIdValue(entity);
        boolean isAutoId = (currentId == null) || (currentId instanceof Number n && n.longValue() == 0);

        Map<String, Field> cols = isAutoId ? meta.nonIdColumns() : meta.columnToField();

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        for (String col : cols.keySet()) {
            columns.add(col);
            placeholders.add("?");
        }

        String sql = "INSERT INTO " + meta.tableName()
                + " (" + columns + ") VALUES (" + placeholders + ")";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = isAutoId
                     ? conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                     : conn.prepareStatement(sql)) {

            int i = 1;
            for (Map.Entry<String, Field> entry : cols.entrySet()) {
                Field field = entry.getValue();
                field.setAccessible(true);
                SqlTypeConverter.setParameter(stmt, i++, field.get(entity));
            }

            stmt.executeUpdate();

            // Populate generated primary key back to the entity
            if (isAutoId) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        Field idField = meta.idField();
                        idField.setAccessible(true);
                        Object generatedKey = keys.getObject(1);

                        if (idField.getType() == int.class || idField.getType() == Integer.class) {
                            idField.set(entity, ((Number) generatedKey).intValue());
                        } else if (idField.getType() == long.class || idField.getType() == Long.class) {
                            idField.set(entity, ((Number) generatedKey).longValue());
                        } else {
                            idField.set(entity, generatedKey);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new SabreException("Failed to save entity of type " + entity.getClass().getName(), e);
        } catch (IllegalAccessException e) {
            throw new SabreException("Cannot read field values for save()", e);
        }
    }

    public <T> T findById(Class<T> type, Object id) {
        EntityMapper.EntityMeta meta = EntityMapper.getMeta(type);
        String sql = "SELECT * FROM " + meta.tableName()
                + " WHERE " + meta.idColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            SqlTypeConverter.setParameter(stmt, 1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return ResultSetMapper.mapOne(rs, type);
            }

        } catch (SQLException e) {
            throw new SabreException("Failed to findById for " + type.getName(), e);
        }
    }

    public <T> List<T> findAll(Class<T> type) {
        EntityMapper.EntityMeta meta = EntityMapper.getMeta(type);
        String sql = "SELECT * FROM " + meta.tableName();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return ResultSetMapper.mapAll(rs, type);

        } catch (SQLException e) {
            throw new SabreException("Failed to findAll for " + type.getName(), e);
        }
    }

    public void update(Object entity) {
        EntityMapper.EntityMeta meta = EntityMapper.getMeta(entity.getClass());
        Map<String, Field> cols = meta.nonIdColumns();

        StringJoiner setClauses = new StringJoiner(", ");
        for (String col : cols.keySet()) {
            setClauses.add(col + " = ?");
        }

        String sql = "UPDATE " + meta.tableName()
                + " SET " + setClauses
                + " WHERE " + meta.idColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (Map.Entry<String, Field> entry : cols.entrySet()) {
                Field field = entry.getValue();
                field.setAccessible(true);
                SqlTypeConverter.setParameter(stmt, i++, field.get(entity));
            }

            // Bind the @Id value as the final parameter for WHERE clause
            SqlTypeConverter.setParameter(stmt, i, EntityMapper.getIdValue(entity));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new SabreException("Failed to update entity of type " + entity.getClass().getName(), e);
        } catch (IllegalAccessException e) {
            throw new SabreException("Cannot read field values for update()", e);
        }
    }

    public <T> void delete(Class<T> type, Object id) {
        EntityMapper.EntityMeta meta = EntityMapper.getMeta(type);
        String sql = "DELETE FROM " + meta.tableName()
                + " WHERE " + meta.idColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            SqlTypeConverter.setParameter(stmt, 1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new SabreException("Failed to delete from " + meta.tableName(), e);
        }
    }
}
