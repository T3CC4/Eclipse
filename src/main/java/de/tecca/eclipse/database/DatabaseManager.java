package de.tecca.eclipse.database;

import de.tecca.eclipse.Eclipse;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Map;
import java.lang.reflect.Type;

/**
 * Professional database manager with SQL and JSON support
 * Supports: MySQL, SQLite (local file), and JSON storage
 */
public class DatabaseManager {

    private HikariDataSource dataSource;
    private final Eclipse plugin;
    private boolean connected = false;
    private DatabaseType databaseType;

    // JSON storage
    private final Gson gson;
    private final Map<String, Object> jsonData;
    private File jsonDataFile;

    public enum DatabaseType {
        MYSQL, SQLITE, JSON
    }

    public DatabaseManager(Eclipse plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.jsonData = new ConcurrentHashMap<>();
    }

    /**
     * Connect to MySQL database
     */
    public boolean connectMySQL(String host, int port, String database, String username, String password) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);

            this.dataSource = new HikariDataSource(config);
            this.connected = testConnection();
            this.databaseType = DatabaseType.MYSQL;

            if (connected) {
                plugin.getLogger().info("Successfully connected to MySQL database");
            }

            return connected;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Connect to SQLite database with custom file path
     */
    public boolean connectSQLite(String filePath) {
        try {
            File sqliteFile = new File(filePath);

            // Create directory if it doesn't exist
            if (!sqliteFile.getParentFile().exists()) {
                sqliteFile.getParentFile().mkdirs();
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");

            // SQLite specific settings
            config.setMaximumPoolSize(1);
            config.setConnectionTimeout(30000);

            this.dataSource = new HikariDataSource(config);
            this.connected = testConnection();
            this.databaseType = DatabaseType.SQLITE;

            if (connected) {
                plugin.getLogger().info("Successfully connected to SQLite database: " + filePath);
            }

            return connected;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Connect to SQLite database in plugin folder
     */
    public boolean connectSQLiteLocal(String fileName) {
        return connectSQLite(plugin.getDataFolder() + "/" + fileName);
    }

    /**
     * Initialize JSON storage
     */
    public boolean connectJSON(String fileName) {
        try {
            this.jsonDataFile = new File(plugin.getDataFolder(), fileName);

            // Create directory if it doesn't exist
            if (!jsonDataFile.getParentFile().exists()) {
                jsonDataFile.getParentFile().mkdirs();
            }

            // Load existing data or create new file
            if (jsonDataFile.exists()) {
                loadJSONData();
            } else {
                jsonDataFile.createNewFile();
                saveJSONData();
            }

            this.connected = true;
            this.databaseType = DatabaseType.JSON;

            plugin.getLogger().info("Successfully initialized JSON storage: " + fileName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize JSON storage: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load JSON data from file
     */
    private void loadJSONData() {
        try {
            if (jsonDataFile.length() == 0) {
                // Empty file, initialize with empty map
                return;
            }

            String jsonContent = Files.readString(jsonDataFile.toPath());
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> loadedData = gson.fromJson(jsonContent, type);

            if (loadedData != null) {
                jsonData.clear();
                jsonData.putAll(loadedData);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load JSON data: " + e.getMessage());
        }
    }

    /**
     * Save JSON data to file
     */
    private void saveJSONData() {
        try {
            String jsonContent = gson.toJson(jsonData);
            Files.writeString(jsonDataFile.toPath(), jsonContent);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save JSON data: " + e.getMessage());
        }
    }

    /**
     * Test database connection
     */
    private boolean testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Get a database connection
     */
    public Connection getConnection() throws SQLException {
        if (!connected || dataSource == null) {
            throw new SQLException("Database not connected");
        }
        return dataSource.getConnection();
    }

    /**
     * Execute an update query (INSERT, UPDATE, DELETE)
     * For JSON storage, this method is not applicable
     */
    public void executeUpdate(String query, Object... params) {
        if (databaseType == DatabaseType.JSON) {
            plugin.getLogger().warning("executeUpdate not supported for JSON storage. Use JSON-specific methods.");
            return;
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Database update error: " + e.getMessage());
        }
    }

    /**
     * Execute an update query asynchronously
     */
    public CompletableFuture<Void> executeUpdateAsync(String query, Object... params) {
        return CompletableFuture.runAsync(() -> executeUpdate(query, params));
    }

    /**
     * Execute a query and process results
     * For JSON storage, this method is not applicable
     */
    public void executeQuery(String query, Consumer<ResultSet> resultProcessor, Object... params) {
        if (databaseType == DatabaseType.JSON) {
            plugin.getLogger().warning("executeQuery not supported for JSON storage. Use JSON-specific methods.");
            return;
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                resultProcessor.accept(resultSet);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database query error: " + e.getMessage());
        }
    }

    /**
     * Execute a query asynchronously and process results
     */
    public CompletableFuture<Void> executeQueryAsync(String query, Consumer<ResultSet> resultProcessor, Object... params) {
        return CompletableFuture.runAsync(() -> executeQuery(query, resultProcessor, params));
    }

    /**
     * Execute a query and return a result
     * For JSON storage, this method is not applicable
     */
    public <T> T executeQueryWithResult(String query, Function<ResultSet, T> resultProcessor, Object... params) {
        if (databaseType == DatabaseType.JSON) {
            plugin.getLogger().warning("executeQueryWithResult not supported for JSON storage. Use JSON-specific methods.");
            return null;
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultProcessor.apply(resultSet);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database query error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Execute a query asynchronously and return a result
     */
    public <T> CompletableFuture<T> executeQueryWithResultAsync(String query, Function<ResultSet, T> resultProcessor, Object... params) {
        return CompletableFuture.supplyAsync(() -> executeQueryWithResult(query, resultProcessor, params));
    }

    /**
     * Create a table if it doesn't exist (SQL only)
     */
    public void createTableIfNotExists(String tableName, String tableStructure) {
        if (databaseType == DatabaseType.JSON) {
            plugin.getLogger().warning("createTableIfNotExists not supported for JSON storage.");
            return;
        }
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " " + tableStructure;
        executeUpdate(query);
    }

    // JSON-specific methods

    /**
     * Store data in JSON storage
     */
    public void setJSONData(String key, Object value) {
        if (databaseType != DatabaseType.JSON) {
            plugin.getLogger().warning("setJSONData only supported for JSON storage.");
            return;
        }
        jsonData.put(key, value);
        saveJSONData();
    }

    /**
     * Store data in JSON storage asynchronously
     */
    public CompletableFuture<Void> setJSONDataAsync(String key, Object value) {
        if (databaseType != DatabaseType.JSON) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> setJSONData(key, value));
    }

    /**
     * Get data from JSON storage
     */
    public <T> T getJSONData(String key, Class<T> type) {
        if (databaseType != DatabaseType.JSON) {
            plugin.getLogger().warning("getJSONData only supported for JSON storage.");
            return null;
        }

        Object value = jsonData.get(key);
        if (value == null) return null;

        // If it's already the correct type, return it
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        // Otherwise, try to convert via JSON
        try {
            String json = gson.toJson(value);
            return gson.fromJson(json, type);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert JSON data for key '" + key + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Get data from JSON storage with default value
     */
    public <T> T getJSONData(String key, Class<T> type, T defaultValue) {
        T result = getJSONData(key, type);
        return result != null ? result : defaultValue;
    }

    /**
     * Get data from JSON storage asynchronously
     */
    public <T> CompletableFuture<T> getJSONDataAsync(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> getJSONData(key, type));
    }

    /**
     * Check if key exists in JSON storage
     */
    public boolean hasJSONData(String key) {
        if (databaseType != DatabaseType.JSON) {
            return false;
        }
        return jsonData.containsKey(key);
    }

    /**
     * Remove data from JSON storage
     */
    public void removeJSONData(String key) {
        if (databaseType != DatabaseType.JSON) {
            plugin.getLogger().warning("removeJSONData only supported for JSON storage.");
            return;
        }
        jsonData.remove(key);
        saveJSONData();
    }

    /**
     * Remove data from JSON storage asynchronously
     */
    public CompletableFuture<Void> removeJSONDataAsync(String key) {
        if (databaseType != DatabaseType.JSON) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> removeJSONData(key));
    }

    /**
     * Clear all JSON data
     */
    public void clearJSONData() {
        if (databaseType != DatabaseType.JSON) {
            plugin.getLogger().warning("clearJSONData only supported for JSON storage.");
            return;
        }
        jsonData.clear();
        saveJSONData();
    }

    /**
     * Get all JSON data keys
     */
    public java.util.Set<String> getJSONDataKeys() {
        if (databaseType != DatabaseType.JSON) {
            return java.util.Set.of();
        }
        return new java.util.HashSet<>(jsonData.keySet());
    }

    /**
     * Force save JSON data to file
     */
    public void saveJSON() {
        if (databaseType == DatabaseType.JSON) {
            saveJSONData();
        }
    }

    /**
     * Force reload JSON data from file
     */
    public void reloadJSON() {
        if (databaseType == DatabaseType.JSON) {
            loadJSONData();
        }
    }

    /**
     * Get the current database type
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        if (databaseType == DatabaseType.JSON) {
            return connected && jsonDataFile != null && jsonDataFile.exists();
        }
        return connected && dataSource != null && !dataSource.isClosed();
    }

    /**
     * Close database connection or JSON storage
     */
    public void disconnect() {
        plugin.getLogger().info("Disconnecting database...");

        if (databaseType == DatabaseType.JSON) {
            if (connected && jsonDataFile != null) {
                try {
                    // Final save of JSON data
                    saveJSONData();
                    plugin.getLogger().info("JSON data saved successfully");

                    // Clear in-memory data
                    int dataCount = jsonData.size();
                    jsonData.clear();

                    connected = false;
                    plugin.getLogger().info("JSON storage closed - " + dataCount + " keys cleared");
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during JSON storage shutdown: " + e.getMessage());
                }
            }
        } else {
            if (dataSource != null && !dataSource.isClosed()) {
                try {
                    // Wait for active connections to close (with timeout)
                    long shutdownStart = System.currentTimeMillis();
                    while (dataSource.getHikariPoolMXBean().getActiveConnections() > 0 &&
                            (System.currentTimeMillis() - shutdownStart) < 10000) {
                        Thread.sleep(100);
                    }

                    // Log final connection stats
                    String finalStats = String.format("Final connection stats - Active: %d, Idle: %d, Total: %d",
                            dataSource.getHikariPoolMXBean().getActiveConnections(),
                            dataSource.getHikariPoolMXBean().getIdleConnections(),
                            dataSource.getHikariPoolMXBean().getTotalConnections());
                    plugin.getLogger().info(finalStats);

                    // Close the datasource
                    dataSource.close();
                    connected = false;

                    plugin.getLogger().info("Database connection pool closed successfully");
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during database shutdown: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get connection pool statistics or JSON storage info
     */
    public String getPoolStats() {
        if (databaseType == DatabaseType.JSON) {
            return String.format("JSON Storage - Keys: %d, File: %s",
                    jsonData.size(),
                    jsonDataFile != null ? jsonDataFile.getName() : "none");
        } else if (dataSource != null) {
            return String.format("Active: %d, Idle: %d, Total: %d, Waiting: %d",
                    dataSource.getHikariPoolMXBean().getActiveConnections(),
                    dataSource.getHikariPoolMXBean().getIdleConnections(),
                    dataSource.getHikariPoolMXBean().getTotalConnections(),
                    dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "Not connected";
    }
}