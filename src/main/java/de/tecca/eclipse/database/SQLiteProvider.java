package de.tecca.eclipse.database;

import de.tecca.eclipse.api.database.Transaction;
import org.bukkit.plugin.Plugin;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;

public class SQLiteProvider implements DatabaseProvider {

    private final Plugin plugin;
    private final String filename;
    private final String filePath;
    private ConnectionManager connectionManager;

    public SQLiteProvider(Plugin plugin, String filename) {
        this.plugin = plugin;
        this.filename = filename;
        this.filePath = new File(plugin.getDataFolder(), filename).getAbsolutePath();

        // Create database file if it doesn't exist
        createDatabaseFile();
    }

    private void createDatabaseFile() {
        File dbFile = new File(filePath);
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            try {
                dbFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create SQLite database file: " + e.getMessage());
            }
        }
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
                throw new RuntimeException("SQLite query failed: " + e.getMessage(), e);
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
                throw new RuntimeException("SQLite update failed: " + e.getMessage(), e);
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
                throw new RuntimeException("SQLite execute failed: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to begin SQLite transaction: " + e.getMessage(), e);
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
    public String getFilename() { return filename; }
    public String getFilePath() { return filePath; }
}