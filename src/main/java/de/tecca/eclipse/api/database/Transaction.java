package de.tecca.eclipse.api.database;

import java.util.concurrent.CompletableFuture;

public interface Transaction {

    Transaction then(Runnable operation);
    Transaction thenQuery(String sql, Object... params);
    Transaction thenUpdate(String sql, Object... params);

    CompletableFuture<Void> commit();
    CompletableFuture<Void> rollback();

    boolean isActive();
    void close();
}