package de.tecca.eclipse.impl;

import de.tecca.eclipse.api.DatabaseAPI;
import de.tecca.eclipse.api.database.*;
import de.tecca.eclipse.database.*;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.List;
import java.util.Map;

public class DatabaseAPIImpl implements DatabaseAPI {

    private final Plugin plugin;
    private ConnectionManager connectionManager;
    private MySQLProvider mysqlProvider;
    private SQLiteProvider sqliteProvider;
    private JSONProvider jsonProvider;
    private DatabaseType currentType = DatabaseType.JSON_ONLY;

    public DatabaseAPIImpl(Plugin plugin) {
        this.plugin = plugin;
        this.jsonProvider = new JSONProvider(plugin);
    }

    @Override
    public QueryBuilder table(String tableName) {
        return new QueryBuilderImpl(tableName, getCurrentProvider());
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... params) {
        return getCurrentProvider().query(sql, params);
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... params) {
        return getCurrentProvider().update(sql, params);
    }

    @Override
    public CompletableFuture<Void> execute(String sql, Object... params) {
        return getCurrentProvider().execute(sql, params);
    }

    @Override
    public <T> CompletableFuture<Void> setJSON(String key, T data) {
        return jsonProvider.setJSON(key, data);
    }

    @Override
    public <T> CompletableFuture<Optional<T>> getJSON(String key, Class<T> type) {
        return jsonProvider.getJSON(key, type);
    }

    @Override
    public CompletableFuture<Void> removeJSON(String key) {
        return jsonProvider.removeJSON(key);
    }

    @Override
    public CompletableFuture<List<String>> listJSONKeys(String prefix) {
        return jsonProvider.listJSONKeys(prefix);
    }

    @Override
    public Transaction beginTransaction() {
        if (currentType == DatabaseType.JSON_ONLY) {
            throw new UnsupportedOperationException("Transactions not supported with JSON storage");
        }
        return getCurrentProvider().beginTransaction();
    }

    @Override
    public DatabaseAPI configureMySQL(String host, int port, String database, String username, String password) {
        this.mysqlProvider = new MySQLProvider(host, port, database, username, password);
        this.connectionManager = new ConnectionManager(mysqlProvider);
        this.currentType = DatabaseType.MYSQL;
        return this;
    }

    @Override
    public DatabaseAPI configureSQLite(String filename) {
        this.sqliteProvider = new SQLiteProvider(plugin, filename);
        this.connectionManager = new ConnectionManager(sqliteProvider);
        this.currentType = DatabaseType.SQLITE;
        return this;
    }

    @Override
    public DatabaseAPI setPoolSize(int minConnections, int maxConnections) {
        if (connectionManager != null) {
            connectionManager.setPoolSize(minConnections, maxConnections);
        }
        return this;
    }

    @Override
    public boolean isConnected() {
        return getCurrentProvider().isConnected();
    }

    @Override
    public DatabaseType getType() {
        return currentType;
    }

    @Override
    public void shutdown() {
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
        if (jsonProvider != null) {
            jsonProvider.shutdown();
        }
    }

    private DatabaseProvider getCurrentProvider() {
        switch (currentType) {
            case MYSQL -> {
                return mysqlProvider;
            }
            case SQLITE -> {
                return sqliteProvider;
            }
            default -> {
                return jsonProvider;
            }
        }
    }
}