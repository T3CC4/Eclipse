package de.tecca.eclipse.database;

import de.tecca.eclipse.api.database.Transaction;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class MySQLProvider implements DatabaseProvider {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private ConnectionManager connectionManager;

    public MySQLProvider(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);

                try (ResultSet rs = stmt.executeQuery()) {
                    return mapResultSet(rs);
                }

            } catch (SQLException e) {
                throw new RuntimeException("Query failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);
                return stmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("Update failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> execute(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);
                stmt.execute();

            } catch (SQLException e) {
                throw new RuntimeException("Execute failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public Transaction beginTransaction() {
        try {
            Connection conn = connectionManager.getConnection();
            conn.setAutoCommit(false);
            return new TransactionImpl(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to begin transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return connectionManager != null && connectionManager.isConnected();
    }

    @Override
    public void shutdown() {
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
    }

    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private List<Map<String, Object>> mapResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }

        return results;
    }

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}