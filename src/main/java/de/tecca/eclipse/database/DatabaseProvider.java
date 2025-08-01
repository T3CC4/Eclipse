package de.tecca.eclipse.database;

import de.tecca.eclipse.api.database.Transaction;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;

public interface DatabaseProvider {
    CompletableFuture<List<Map<String, Object>>> query(String sql, Object... params);
    CompletableFuture<Integer> update(String sql, Object... params);
    CompletableFuture<Void> execute(String sql, Object... params);
    Transaction beginTransaction();
    boolean isConnected();
    void shutdown();
}