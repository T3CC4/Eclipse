package de.tecca.eclipse.database;

import de.tecca.eclipse.api.database.QueryBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class QueryBuilderImpl implements QueryBuilder {

    private final String tableName;
    private final DatabaseProvider provider;

    private String operation = "SELECT";
    private List<String> selectColumns = new ArrayList<>();
    private List<String> whereConditions = new ArrayList<>();
    private List<Object> parameters = new ArrayList<>();
    private Map<String, Object> values = new HashMap<>();
    private String orderByColumn;
    private String orderByDirection = "ASC";
    private Integer limitCount;
    private Integer offsetCount;

    public QueryBuilderImpl(String tableName, DatabaseProvider provider) {
        this.tableName = tableName;
        this.provider = provider;
    }

    @Override
    public QueryBuilder select(String... columns) {
        operation = "SELECT";
        selectColumns.addAll(List.of(columns));
        return this;
    }

    @Override
    public QueryBuilder where(String column, Object value) {
        return where(column, "=", value);
    }

    @Override
    public QueryBuilder where(String column, String operator, Object value) {
        whereConditions.add(column + " " + operator + " ?");
        parameters.add(value);
        return this;
    }

    @Override
    public QueryBuilder whereIn(String column, Object... values) {
        String placeholders = "?,".repeat(values.length);
        placeholders = placeholders.substring(0, placeholders.length() - 1);
        whereConditions.add(column + " IN (" + placeholders + ")");
        parameters.addAll(List.of(values));
        return this;
    }

    @Override
    public QueryBuilder whereNotNull(String column) {
        whereConditions.add(column + " IS NOT NULL");
        return this;
    }

    @Override
    public QueryBuilder orderBy(String column) {
        return orderBy(column, "ASC");
    }

    @Override
    public QueryBuilder orderBy(String column, String direction) {
        this.orderByColumn = column;
        this.orderByDirection = direction;
        return this;
    }

    @Override
    public QueryBuilder limit(int limit) {
        this.limitCount = limit;
        return this;
    }

    @Override
    public QueryBuilder offset(int offset) {
        this.offsetCount = offset;
        return this;
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> get() {
        String sql = buildSelectSQL();
        return provider.query(sql, parameters.toArray());
    }

    @Override
    public CompletableFuture<Optional<Map<String, Object>>> first() {
        limit(1);
        String sql = buildSelectSQL();
        return provider.query(sql, parameters.toArray())
                .thenApply(results -> results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)));
    }

    @Override
    public CompletableFuture<Long> count() {
        String sql = "SELECT COUNT(*) as count FROM " + tableName;
        if (!whereConditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", whereConditions);
        }
        return provider.query(sql, parameters.toArray())
                .thenApply(results -> ((Number) results.get(0).get("count")).longValue());
    }

    @Override
    public QueryBuilder insert() {
        operation = "INSERT";
        return this;
    }

    @Override
    public QueryBuilder insertOrUpdate() {
        operation = "INSERT_OR_UPDATE";
        return this;
    }

    @Override
    public QueryBuilder value(String column, Object value) {
        values.put(column, value);
        return this;
    }

    @Override
    public QueryBuilder values(Map<String, Object> values) {
        this.values.putAll(values);
        return this;
    }

    @Override
    public QueryBuilder update() {
        operation = "UPDATE";
        return this;
    }

    @Override
    public QueryBuilder set(String column, Object value) {
        return value(column, value);
    }

    @Override
    public QueryBuilder increment(String column, Number amount) {
        values.put(column, column + " + " + amount);
        return this;
    }

    @Override
    public QueryBuilder decrement(String column, Number amount) {
        values.put(column, column + " - " + amount);
        return this;
    }

    @Override
    public QueryBuilder delete() {
        operation = "DELETE";
        return this;
    }

    @Override
    public CompletableFuture<Integer> execute() {
        String sql = buildSQL();
        Object[] params = buildParameters();
        return provider.update(sql, params);
    }

    @Override
    public CompletableFuture<Void> executeVoid() {
        String sql = buildSQL();
        Object[] params = buildParameters();
        return provider.execute(sql, params);
    }

    @Override
    public QueryBuilder batch() {
        // Implementation for batch operations
        return this;
    }

    @Override
    public QueryBuilder addBatch() {
        // Implementation for adding to batch
        return this;
    }

    @Override
    public CompletableFuture<int[]> executeBatch() {
        // Implementation for batch execution
        return CompletableFuture.completedFuture(new int[0]);
    }

    @Override
    public List<Map<String, Object>> sync() {
        try {
            return get().get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public Optional<Map<String, Object>> syncFirst() {
        try {
            return first().get();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public int syncExecute() {
        try {
            return execute().get();
        } catch (Exception e) {
            return 0;
        }
    }

    private String buildSelectSQL() {
        StringBuilder sql = new StringBuilder("SELECT ");

        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectColumns));
        }

        sql.append(" FROM ").append(tableName);

        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
        }

        if (orderByColumn != null) {
            sql.append(" ORDER BY ").append(orderByColumn).append(" ").append(orderByDirection);
        }

        if (limitCount != null) {
            sql.append(" LIMIT ").append(limitCount);
        }

        if (offsetCount != null) {
            sql.append(" OFFSET ").append(offsetCount);
        }

        return sql.toString();
    }

    private String buildSQL() {
        return switch (operation) {
            case "INSERT" -> buildInsertSQL();
            case "INSERT_OR_UPDATE" -> buildInsertOrUpdateSQL();
            case "UPDATE" -> buildUpdateSQL();
            case "DELETE" -> buildDeleteSQL();
            default -> buildSelectSQL();
        };
    }

    private String buildInsertSQL() {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName);

        if (!values.isEmpty()) {
            List<String> columns = new ArrayList<>(values.keySet());
            sql.append(" (").append(String.join(", ", columns)).append(")");
            sql.append(" VALUES (").append("?,".repeat(columns.size()));
            sql.setLength(sql.length() - 1);
            sql.append(")");
        }

        return sql.toString();
    }

    private String buildInsertOrUpdateSQL() {
        return buildInsertSQL() + " ON DUPLICATE KEY UPDATE " +
                String.join(", ", values.keySet().stream()
                        .map(col -> col + " = VALUES(" + col + ")")
                        .toList());
    }

    private String buildUpdateSQL() {
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName);

        if (!values.isEmpty()) {
            sql.append(" SET ").append(String.join(", ",
                    values.keySet().stream().map(col -> col + " = ?").toList()));
        }

        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
        }

        return sql.toString();
    }

    private String buildDeleteSQL() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName);

        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
        }

        return sql.toString();
    }

    private Object[] buildParameters() {
        List<Object> allParams = new ArrayList<>();

        if (operation.equals("INSERT") || operation.equals("INSERT_OR_UPDATE")) {
            allParams.addAll(values.values());
        } else if (operation.equals("UPDATE")) {
            allParams.addAll(values.values());
            allParams.addAll(parameters);
        } else {
            allParams.addAll(parameters);
        }

        return allParams.toArray();
    }
}