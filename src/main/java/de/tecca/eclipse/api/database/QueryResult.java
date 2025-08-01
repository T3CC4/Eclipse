package de.tecca.eclipse.api.database;

public class QueryResult {
    private final Object value;
    private final String columnName;

    public QueryResult(Object value, String columnName) {
        this.value = value;
        this.columnName = columnName;
    }

    public String getString() {
        return value != null ? value.toString() : null;
    }

    public Integer getInt() {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    public Long getLong() {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    public Double getDouble() {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    public Boolean getBoolean() {
        return value instanceof Boolean ? (Boolean) value : null;
    }

    public Object getValue() {
        return value;
    }

    public String getColumnName() {
        return columnName;
    }
}