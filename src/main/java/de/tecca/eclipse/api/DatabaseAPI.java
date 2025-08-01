package de.tecca.eclipse.api;

import de.tecca.eclipse.api.database.*;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.List;
import java.util.Map;

public interface DatabaseAPI {

    QueryBuilder table(String tableName);

    CompletableFuture<List<Map<String, Object>>> query(String sql, Object... params);
    CompletableFuture<Integer> update(String sql, Object... params);
    CompletableFuture<Void> execute(String sql, Object... params);

    <T> CompletableFuture<Void> setJSON(String key, T data);
    <T> CompletableFuture<Optional<T>> getJSON(String key, Class<T> type);
    CompletableFuture<Void> removeJSON(String key);
    CompletableFuture<List<String>> listJSONKeys(String prefix);

    Transaction beginTransaction();

    DatabaseAPI configureMySQL(String host, int port, String database, String username, String password);
    DatabaseAPI configureSQLite(String filename);
    DatabaseAPI setPoolSize(int minConnections, int maxConnections);

    boolean isConnected();
    DatabaseType getType();
    void shutdown();

    enum DatabaseType {
        MYSQL, SQLITE, JSON_ONLY
    }
}