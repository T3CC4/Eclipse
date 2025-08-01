package de.tecca.eclipse.database;

import de.tecca.eclipse.api.database.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

public class TransactionImpl implements Transaction {

    private final Connection connection;
    private final List<TransactionOperation> operations = new ArrayList<>();
    private boolean active = true;

    public TransactionImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Transaction then(Runnable operation) {
        if (!active) {
            throw new IllegalStateException("Transaction is not active");
        }

        operations.add(new TransactionOperation("RUNNABLE", null, null, operation));
        return this;
    }

    @Override
    public Transaction thenQuery(String sql, Object... params) {
        if (!active) {
            throw new IllegalStateException("Transaction is not active");
        }

        operations.add(new TransactionOperation("QUERY", sql, params, null));
        return this;
    }

    @Override
    public Transaction thenUpdate(String sql, Object... params) {
        if (!active) {
            throw new IllegalStateException("Transaction is not active");
        }

        operations.add(new TransactionOperation("UPDATE", sql, params, null));
        return this;
    }

    @Override
    public CompletableFuture<Void> commit() {
        return CompletableFuture.runAsync(() -> {
            if (!active) {
                throw new IllegalStateException("Transaction is not active");
            }

            try {
                executeOperations();
                connection.commit();
                active = false;
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    // Log rollback failure
                }
                throw new RuntimeException("Transaction commit failed: " + e.getMessage(), e);
            } finally {
                close();
            }
        });
    }

    @Override
    public CompletableFuture<Void> rollback() {
        return CompletableFuture.runAsync(() -> {
            if (!active) {
                return;
            }

            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new RuntimeException("Transaction rollback failed: " + e.getMessage(), e);
            } finally {
                active = false;
                close();
            }
        });
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                // Log close failure
            }
        }
        active = false;
    }

    private void executeOperations() throws SQLException {
        for (TransactionOperation operation : operations) {
            switch (operation.type) {
                case "RUNNABLE" -> operation.runnable.run();
                case "QUERY" -> executeQuery(operation.sql, operation.params);
                case "UPDATE" -> executeUpdate(operation.sql, operation.params);
            }
        }
    }

    private void executeQuery(String sql, Object[] params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            stmt.executeQuery();
        }
    }

    private void executeUpdate(String sql, Object[] params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            stmt.executeUpdate();
        }
    }

    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    private static class TransactionOperation {
        final String type;
        final String sql;
        final Object[] params;
        final Runnable runnable;

        TransactionOperation(String type, String sql, Object[] params, Runnable runnable) {
            this.type = type;
            this.sql = sql;
            this.params = params;
            this.runnable = runnable;
        }
    }
}