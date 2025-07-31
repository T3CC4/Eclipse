package de.tecca.eclipse;

import de.tecca.eclipse.api.EclipseAPI;
import de.tecca.eclipse.cache.PlayerCache;
import de.tecca.eclipse.config.ConfigManager;
import de.tecca.eclipse.database.DatabaseManager;
import de.tecca.eclipse.event.EventManager;
import de.tecca.eclipse.message.MessageManager;
import de.tecca.eclipse.migration.MigrationSystem;
import de.tecca.eclipse.permission.PermissionManager;
import de.tecca.eclipse.queue.TaskQueue;
import de.tecca.eclipse.security.SecurityManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Eclipse Framework - A professional, minimal plugin framework
 * Designed to be used as both a standalone plugin and library
 */
public final class Eclipse extends JavaPlugin {

    private static Eclipse instance;
    private static EclipseAPI api;

    private EventManager eventManager;
    private TaskQueue taskQueue;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private PlayerCache playerCache;
    private PermissionManager permissionManager;
    private SecurityManager securityManager;
    private MigrationSystem migrationSystem;

    @Override
    public void onEnable() {
        instance = this;

        try {
            getLogger().info("Starting Eclipse Framework initialization...");
            long startTime = System.currentTimeMillis();

            // Initialize core components in dependency order
            initializeCore();
            initializeStorage();
            initializeManagers();
            initializeAdvancedSystems();

            // Initialize API with all managers
            initializeAPI();

            // Run post-initialization tasks
            postInitialization();

            long initTime = System.currentTimeMillis() - startTime;
            getLogger().info("Eclipse Framework enabled successfully in " + initTime + "ms");
            getLogger().info("Available systems: Config, Event, Task, Database, Message, PlayerCache, Permission, Security, Migration");

        } catch (Exception e) {
            getLogger().severe("Failed to initialize Eclipse Framework: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initialize core systems
     */
    private void initializeCore() {
        getLogger().info("Initializing core systems...");

        this.configManager = new ConfigManager(this);
        getLogger().info("✓ Configuration Manager initialized");

        this.eventManager = new EventManager(this);
        getLogger().info("✓ Event Manager initialized");

        this.taskQueue = new TaskQueue(this);
        getLogger().info("✓ Task Queue initialized");
    }

    /**
     * Initialize storage systems
     */
    private void initializeStorage() {
        getLogger().info("Initializing storage systems...");

        this.databaseManager = new DatabaseManager(this);
        getLogger().info("✓ Database Manager initialized");

        // Auto-initialize database based on config
        autoInitializeDatabase();
    }

    /**
     * Initialize standard managers
     */
    private void initializeManagers() {
        getLogger().info("Initializing managers...");

        this.messageManager = new MessageManager(this, configManager);
        getLogger().info("✓ Message Manager initialized");

        this.playerCache = new PlayerCache(this);
        getLogger().info("✓ Player Cache initialized");
    }

    /**
     * Initialize advanced systems
     */
    private void initializeAdvancedSystems() {
        getLogger().info("Initializing advanced systems...");

        this.permissionManager = new PermissionManager(this, databaseManager);
        getLogger().info("✓ Permission Manager initialized");

        this.securityManager = new SecurityManager(this, databaseManager);
        getLogger().info("✓ Security Manager initialized");

        this.migrationSystem = new MigrationSystem(this, databaseManager);
        getLogger().info("✓ Migration System initialized");
    }

    /**
     * Initialize the unified API
     */
    private void initializeAPI() {
        api = new EclipseAPI(this, eventManager, taskQueue, configManager,
                databaseManager, messageManager, playerCache,
                permissionManager, securityManager, migrationSystem);
        getLogger().info("✓ Eclipse API initialized with all managers");
    }

    /**
     * Auto-initialize database based on configuration
     */
    private void autoInitializeDatabase() {
        var config = configManager.loadConfig("database", true);

        String type = config.getString("type", "json").toLowerCase();

        switch (type) {
            case "mysql" -> {
                String host = config.getString("mysql.host", "localhost");
                int port = config.getInt("mysql.port", 3306);
                String database = config.getString("mysql.database", "eclipse");
                String username = config.getString("mysql.username", "root");
                String password = config.getString("mysql.password", "");

                if (databaseManager.connectMySQL(host, port, database, username, password)) {
                    getLogger().info("✓ Connected to MySQL database");
                } else {
                    getLogger().warning("Failed to connect to MySQL, falling back to JSON storage");
                    databaseManager.connectJSON("data.json");
                }
            }
            case "sqlite" -> {
                String filename = config.getString("sqlite.filename", "eclipse.db");
                if (databaseManager.connectSQLiteLocal(filename)) {
                    getLogger().info("✓ Connected to SQLite database");
                } else {
                    getLogger().warning("Failed to connect to SQLite, falling back to JSON storage");
                    databaseManager.connectJSON("data.json");
                }
            }
            default -> {
                String filename = config.getString("json.filename", "data.json");
                if (databaseManager.connectJSON(filename)) {
                    getLogger().info("✓ Using JSON storage");
                }
            }
        }
    }

    /**
     * Run post-initialization tasks
     */
    private void postInitialization() {
        getLogger().info("Running post-initialization tasks...");

        try {
            // Run pending migrations
            migrationSystem.runMigrations().thenAccept(result -> {
                if (result.isSuccess()) {
                    getLogger().info("✓ Migrations completed: " + result.getTotalExecuted() + " executed");
                } else {
                    getLogger().warning("Migration issues: " + result.getTotalFailed() + " failed");
                }
            }).exceptionally(throwable -> {
                getLogger().warning("Migration error: " + throwable.getMessage());
                return null;
            });

            // Perform health check
            taskQueue.addTask(() -> {
                var healthCheck = api.performHealthCheck();
                if (healthCheck.isHealthy()) {
                    getLogger().info("✓ Health check passed: " + healthCheck.getHealthyCount() + "/" + healthCheck.getTotalCount() + " systems healthy");
                } else {
                    getLogger().warning("Health check issues detected: " + healthCheck);
                }
            }, 20L); // Run after 1 second

        } catch (Exception e) {
            getLogger().warning("Post-initialization tasks failed: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Starting Eclipse Framework shutdown...");
        long shutdownStart = System.currentTimeMillis();

        // Shutdown in reverse order of initialization with error handling
        shutdownAdvancedSystems();
        shutdownManagers();
        shutdownStorage();
        shutdownCore();

        // Final cleanup
        api = null;
        instance = null;

        long shutdownTime = System.currentTimeMillis() - shutdownStart;
        getLogger().info("Eclipse Framework shutdown completed in " + shutdownTime + "ms");
    }

    /**
     * Shutdown advanced systems
     */
    private void shutdownAdvancedSystems() {
        try {
            if (migrationSystem != null) {
                migrationSystem.shutdown();
                getLogger().info("✓ Migration System shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during migration system shutdown: " + e.getMessage());
        }

        try {
            if (securityManager != null) {
                securityManager.shutdown();
                getLogger().info("✓ Security Manager shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during security manager shutdown: " + e.getMessage());
        }

        try {
            if (permissionManager != null) {
                permissionManager.shutdown();
                getLogger().info("✓ Permission Manager shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during permission manager shutdown: " + e.getMessage());
        }
    }

    /**
     * Shutdown standard managers
     */
    private void shutdownManagers() {
        try {
            // Save all player data first (most important)
            if (playerCache != null) {
                getLogger().info("Saving all player data...");
                playerCache.saveAll().get(30, java.util.concurrent.TimeUnit.SECONDS);
                playerCache.shutdown();
                getLogger().info("✓ Player Cache shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during player cache shutdown: " + e.getMessage());
        }

        try {
            if (messageManager != null) {
                messageManager.shutdown();
                getLogger().info("✓ Message Manager shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during message manager shutdown: " + e.getMessage());
        }
    }

    /**
     * Shutdown storage systems
     */
    private void shutdownStorage() {
        try {
            if (databaseManager != null) {
                databaseManager.disconnect();
                getLogger().info("✓ Database Manager shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during database shutdown: " + e.getMessage());
        }
    }

    /**
     * Shutdown core systems
     */
    private void shutdownCore() {
        try {
            if (taskQueue != null) {
                taskQueue.shutdown();
                getLogger().info("✓ Task Queue shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during task queue shutdown: " + e.getMessage());
        }

        try {
            if (eventManager != null) {
                eventManager.shutdown();
                getLogger().info("✓ Event Manager shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during event manager shutdown: " + e.getMessage());
        }

        try {
            if (configManager != null) {
                configManager.shutdown();
                getLogger().info("✓ Configuration Manager shutdown complete");
            }
        } catch (Exception e) {
            getLogger().severe("✗ Error during config manager shutdown: " + e.getMessage());
        }
    }

    /**
     * Get the plugin instance
     * @return Eclipse instance
     */
    public static Eclipse getInstance() {
        return instance;
    }

    /**
     * Get the Eclipse API
     * @return EclipseAPI instance
     */
    public static EclipseAPI getAPI() {
        return api;
    }

    /**
     * Get the event manager
     * @return EventManager instance
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Get the task queue
     * @return TaskQueue instance
     */
    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    /**
     * Get the configuration manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the database manager
     * @return DatabaseManager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Get the message manager
     * @return MessageManager instance
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Get the player cache
     * @return PlayerCache instance
     */
    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    /**
     * Get the permission manager
     * @return PermissionManager instance
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * Get the security manager
     * @return SecurityManager instance
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     * Get the migration system
     * @return MigrationSystem instance
     */
    public MigrationSystem getMigrationSystem() {
        return migrationSystem;
    }
}