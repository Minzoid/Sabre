package moon.studio.sabre.core;

import moon.studio.sabre.mapping.Repository;
import moon.studio.sabre.query.QueryBuilder;
import moon.studio.sabre.util.ResultSetMapper;
import moon.studio.sabre.util.SqlTypeConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Sabre implements AutoCloseable {

    private final ConnectionManager connectionManager;
    private final Repository repository;

    private Sabre(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.repository = new Repository(connectionManager);
    }

    public static Sabre connect(String jdbcUrl, String username, String password) {
        return new Sabre(new ConnectionManager(jdbcUrl, username, password));
    }

    public static Sabre connect(String jdbcUrl) {
        return connect(jdbcUrl, null, null);
    }

    public void save(Object entity) {
        repository.save(entity);
    }

    public <T> T findById(Class<T> type, Object id) {
        return repository.findById(type, id);
    }

    public <T> List<T> findAll(Class<T> type) {
        return repository.findAll(type);
    }

    public void update(Object entity) {
        repository.update(entity);
    }

    public <T> void delete(Class<T> type, Object id) {
        repository.delete(type, id);
    }

    public <T> QueryBuilder<T> query(Class<T> type) {
        return new QueryBuilder<>(type, connectionManager);
    }

    public <T> List<T> raw(Class<T> type, String sql, Object... params) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                SqlTypeConverter.setParameter(stmt, i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return ResultSetMapper.mapAll(rs, type);
            }

        } catch (SQLException e) {
            throw new SabreException("Raw query failed: " + sql, e);
        }
    }

    public int execute(String sql, Object... params) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                SqlTypeConverter.setParameter(stmt, i + 1, params[i]);
            }

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new SabreException("Execute failed: " + sql, e);
        }
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @Override
    public void close() {
        connectionManager.close();
    }
}
