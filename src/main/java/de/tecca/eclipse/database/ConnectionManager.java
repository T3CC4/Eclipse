package de.tecca.eclipse.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionManager {

    private final DatabaseProvider provider;
    private HikariDataSource dataSource;
    private ExecutorService executor;
    private boolean initialized = false;

    public ConnectionManager(DatabaseProvider provider) {
        this.provider = provider;
        this.executor = Executors.newCachedThreadPool();
        initialize();
    }

    private void initialize() {
        if (provider instanceof MySQLProvider mysqlProvider) {
            initializeMySQL(mysqlProvider);
        } else if (provider instanceof SQLiteProvider sqliteProvider) {
            initializeSQLite(sqliteProvider);
        }
        initialized = true;
    }

    private void initializeMySQL(MySQLProvider mysqlProvider) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + mysqlProvider.getHost() + ":" +
                mysqlProvider.getPort() + "/" + mysqlProvider.getDatabase());
        config.setUsername(mysqlProvider.getUsername());
        config.setPassword(mysqlProvider.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
    }

    private void initializeSQLite(SQLiteProvider sqliteProvider) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + sqliteProvider.getFilePath());
        config.setDriverClassName("org.sqlite.JDBC");

        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);

        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null) {
            throw new SQLException("Connection manager not initialized");
        }
        return dataSource.getConnection();
    }

    public CompletableFuture<Connection> getConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void setPoolSize(int minConnections, int maxConnections) {
        if (dataSource != null) {
            dataSource.setMinimumIdle(minConnections);
            dataSource.setMaximumPoolSize(maxConnections);
        }
    }

    public boolean isConnected() {
        if (dataSource == null) return false;

        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public int getActiveConnections() {
        return dataSource != null ? dataSource.getHikariPoolMXBean().getActiveConnections() : 0;
    }

    public int getIdleConnections() {
        return dataSource != null ? dataSource.getHikariPoolMXBean().getIdleConnections() : 0;
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}