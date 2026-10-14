package moon.studio.sabre.query;

import moon.studio.sabre.core.ConnectionManager;
import moon.studio.sabre.core.SabreException;
import moon.studio.sabre.mapping.EntityMapper;
import moon.studio.sabre.util.ResultSetMapper;
import moon.studio.sabre.util.SqlTypeConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class QueryBuilder<T> {

    private final Class<T> type;
    private final ConnectionManager connectionManager;
    private final EntityMapper.EntityMeta meta;

    private final List<Condition> conditions = new ArrayList<>();
    private final List<String> orderByClauses = new ArrayList<>();
    private Integer limitValue;
    private Integer offsetValue;

    public QueryBuilder(Class<T> type, ConnectionManager connectionManager) {
        this.type = type;
        this.connectionManager = connectionManager;
        this.meta = EntityMapper.getMeta(type);
    }

    public QueryBuilder<T> where(String column, String operator, Object value) {
        conditions.add(new Condition(column, operator, value));
        return this;
    }

    public QueryBuilder<T> where(String column, Object value) {
        return where(column, "=", value);
    }

    public QueryBuilder<T> orderBy(String column) {
        orderByClauses.add(column + " ASC");
        return this;
    }

    public QueryBuilder<T> orderBy(String column, String direction) {
        orderByClauses.add(column + " " + direction.toUpperCase());
        return this;
    }

    public QueryBuilder<T> limit(int limit) {
        this.limitValue = limit;
        return this;
    }

    public QueryBuilder<T> offset(int offset) {
        this.offsetValue = offset;
        return this;
    }

    public List<T> list() {
        String sql = buildSql();
        List<Object> params = collectParams();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            bindParams(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                return ResultSetMapper.mapAll(rs, type);
            }

        } catch (SQLException e) {
            throw new SabreException("Query failed: " + sql, e);
        }
    }

    public T first() {
        this.limitValue = 1;
        List<T> results = list();
        return results.isEmpty() ? null : results.get(0);
    }

    public long count() {
        String sql = buildCountSql();
        List<Object> params = collectParams();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            bindParams(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }

        } catch (SQLException e) {
            throw new SabreException("Count query failed: " + sql, e);
        }
    }

    private String buildSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(meta.tableName());
        appendWhere(sb);
        appendOrderBy(sb);
        appendLimitOffset(sb);
        return sb.toString();
    }

    private String buildCountSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) FROM ").append(meta.tableName());
        appendWhere(sb);
        return sb.toString();
    }

    private void appendWhere(StringBuilder sb) {
        if (!conditions.isEmpty()) {
            sb.append(" WHERE ");
            StringJoiner joiner = new StringJoiner(" AND ");
            for (Condition c : conditions) {
                joiner.add(c.toSql());
            }
            sb.append(joiner);
        }
    }

    private void appendOrderBy(StringBuilder sb) {
        if (!orderByClauses.isEmpty()) {
            sb.append(" ORDER BY ").append(String.join(", ", orderByClauses));
        }
    }

    private void appendLimitOffset(StringBuilder sb) {
        if (limitValue != null) sb.append(" LIMIT ").append(limitValue);
        if (offsetValue != null) sb.append(" OFFSET ").append(offsetValue);
    }

    private List<Object> collectParams() {
        List<Object> params = new ArrayList<>();
        for (Condition c : conditions) {
            params.add(c.value());
        }
        return params;
    }

    private void bindParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            SqlTypeConverter.setParameter(stmt, i + 1, params.get(i));
        }
    }
}
