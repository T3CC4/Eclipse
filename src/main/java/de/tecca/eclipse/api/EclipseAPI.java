package de.tecca.eclipse.api;

import de.tecca.eclipse.Eclipse;
import de.tecca.eclipse.cache.PlayerCache;
import de.tecca.eclipse.config.ConfigManager;
import de.tecca.eclipse.database.DatabaseManager;
import de.tecca.eclipse.event.EventManager;
import de.tecca.eclipse.event.EclipseEvent;
import de.tecca.eclipse.event.EventListener;
import de.tecca.eclipse.message.MessageManager;
import de.tecca.eclipse.migration.MigrationSystem;
import de.tecca.eclipse.migration.MigrationStatus;
import de.tecca.eclipse.migration.MigrationResult;
import de.tecca.eclipse.permission.PermissionManager;
import de.tecca.eclipse.permission.PermissionGroup;
import de.tecca.eclipse.permission.PlayerPermissionData;
import de.tecca.eclipse.queue.TaskQueue;
import de.tecca.eclipse.queue.Task;
import de.tecca.eclipse.security.SecurityManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Complete API class for Eclipse Framework
 * Provides unified access to all framework functionality
 */
public class EclipseAPI {

    private final Eclipse plugin;
    private final EventManager eventManager;
    private final TaskQueue taskQueue;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final MessageManager messageManager;
    private final PlayerCache playerCache;
    private final PermissionManager permissionManager;
    private final SecurityManager securityManager;
    private final MigrationSystem migrationSystem;

    public EclipseAPI(Eclipse plugin, EventManager eventManager, TaskQueue taskQueue,
                      ConfigManager configManager, DatabaseManager databaseManager,
                      MessageManager messageManager, PlayerCache playerCache,
                      PermissionManager permissionManager, SecurityManager securityManager,
                      MigrationSystem migrationSystem) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.taskQueue = taskQueue;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.messageManager = messageManager;
        this.playerCache = playerCache;
        this.permissionManager = permissionManager;
        this.securityManager = securityManager;
        this.migrationSystem = migrationSystem;
    }

    // ===============================
    // EVENT MANAGER METHODS
    // ===============================

    /**
     * Register an event listener
     * @param listener The listener to register
     */
    public void registerListener(EventListener listener) {
        eventManager.registerListener(listener);
    }

    /**
     * Unregister an event listener
     * @param listener The listener to unregister
     */
    public void unregisterListener(EventListener listener) {
        eventManager.unregisterListener(listener);
    }

    /**
     * Fire an Eclipse event
     * @param event The event to fire
     */
    public void fireEvent(EclipseEvent event) {
        eventManager.fireEvent(event);
    }

    /**
     * Fire an Eclipse event asynchronously
     * @param event The event to fire
     */
    public void fireEventAsync(EclipseEvent event) {
        eventManager.fireEventAsync(event);
    }

    /**
     * Get the number of listeners for an event type
     * @param eventType The event type
     * @return Number of listeners
     */
    public int getListenerCount(Class<? extends EclipseEvent> eventType) {
        return eventManager.getListenerCount(eventType);
    }

    // ===============================
    // TASK QUEUE METHODS
    // ===============================

    /**
     * Queue a task for execution
     * @param task The task to queue
     */
    public void queueTask(Task task) {
        taskQueue.addTask(task);
    }

    /**
     * Queue a task with delay
     * @param task The task to queue
     * @param delayTicks Delay in ticks
     */
    public void queueTask(Task task, long delayTicks) {
        taskQueue.addTask(task, delayTicks);
    }

    /**
     * Queue an async task
     * @param task The task to queue
     */
    public void queueAsyncTask(Task task) {
        taskQueue.addAsyncTask(task);
    }

    /**
     * Queue an async task with delay
     * @param task The task to queue
     * @param delayTicks Delay in ticks
     */
    public void queueAsyncTask(Task task, long delayTicks) {
        taskQueue.addAsyncTask(task, delayTicks);
    }

    /**
     * Get current queue size
     * @return Number of tasks in queue
     */
    public int getQueueSize() {
        return taskQueue.getQueueSize();
    }

    /**
     * Clear all queued tasks
     */
    public void clearQueue() {
        taskQueue.clearQueue();
    }

    // ===============================
    // CONFIG MANAGER METHODS
    // ===============================

    /**
     * Load a configuration file
     * @param fileName Name of the config file
     * @param createDefault Whether to create default if not exists
     * @return FileConfiguration instance
     */
    public FileConfiguration loadConfig(String fileName, boolean createDefault) {
        return configManager.loadConfig(fileName, createDefault);
    }

    /**
     * Get a loaded configuration
     * @param fileName Name of the config file
     * @return FileConfiguration or null if not loaded
     */
    public FileConfiguration getConfig(String fileName) {
        return configManager.getConfig(fileName);
    }

    /**
     * Save a configuration file
     * @param fileName Name of the config file
     */
    public void saveConfig(String fileName) {
        configManager.saveConfig(fileName);
    }

    /**
     * Reload a configuration file
     * @param fileName Name of the config file
     */
    public void reloadConfig(String fileName) {
        configManager.reloadConfig(fileName);
    }

    /**
     * Get string value from config with default
     */
    public String getConfigString(String configName, String path, String defaultValue) {
        return configManager.getString(configName, path, defaultValue);
    }

    /**
     * Get integer value from config with default
     */
    public int getConfigInt(String configName, String path, int defaultValue) {
        return configManager.getInt(configName, path, defaultValue);
    }

    /**
     * Get boolean value from config with default
     */
    public boolean getConfigBoolean(String configName, String path, boolean defaultValue) {
        return configManager.getBoolean(configName, path, defaultValue);
    }

    /**
     * Set a value in configuration
     */
    public void setConfigValue(String configName, String path, Object value) {
        configManager.set(configName, path, value);
    }

    // ===============================
    // DATABASE MANAGER METHODS
    // ===============================

    /**
     * Connect to MySQL database
     */
    public boolean connectMySQL(String host, int port, String database, String username, String password) {
        return databaseManager.connectMySQL(host, port, database, username, password);
    }

    /**
     * Connect to SQLite database with custom path
     */
    public boolean connectSQLite(String filePath) {
        return databaseManager.connectSQLite(filePath);
    }

    /**
     * Connect to SQLite database in plugin folder
     */
    public boolean connectSQLiteLocal(String fileName) {
        return databaseManager.connectSQLiteLocal(fileName);
    }

    /**
     * Initialize JSON storage
     */
    public boolean connectJSON(String fileName) {
        return databaseManager.connectJSON(fileName);
    }

    /**
     * Execute SQL update query (MySQL/SQLite only)
     */
    public void executeUpdate(String query, Object... params) {
        databaseManager.executeUpdate(query, params);
    }

    /**
     * Execute SQL update query asynchronously (MySQL/SQLite only)
     */
    public CompletableFuture<Void> executeUpdateAsync(String query, Object... params) {
        return databaseManager.executeUpdateAsync(query, params);
    }

    /**
     * Store data in JSON storage
     */
    public void setJSONData(String key, Object value) {
        databaseManager.setJSONData(key, value);
    }

    /**
     * Store data in JSON storage asynchronously
     */
    public CompletableFuture<Void> setJSONDataAsync(String key, Object value) {
        return databaseManager.setJSONDataAsync(key, value);
    }

    /**
     * Get data from JSON storage
     */
    public <T> T getJSONData(String key, Class<T> type) {
        return databaseManager.getJSONData(key, type);
    }

    /**
     * Get data from JSON storage with default value
     */
    public <T> T getJSONData(String key, Class<T> type, T defaultValue) {
        return databaseManager.getJSONData(key, type, defaultValue);
    }

    /**
     * Get data from JSON storage asynchronously
     */
    public <T> CompletableFuture<T> getJSONDataAsync(String key, Class<T> type) {
        return databaseManager.getJSONDataAsync(key, type);
    }

    /**
     * Check if key exists in JSON storage
     */
    public boolean hasJSONData(String key) {
        return databaseManager.hasJSONData(key);
    }

    /**
     * Remove data from JSON storage
     */
    public void removeJSONData(String key) {
        databaseManager.removeJSONData(key);
    }

    /**
     * Get database type
     */
    public DatabaseManager.DatabaseType getDatabaseType() {
        return databaseManager.getDatabaseType();
    }

    /**
     * Check if database is connected
     */
    public boolean isDatabaseConnected() {
        return databaseManager.isConnected();
    }

    // ===============================
    // MESSAGE MANAGER METHODS
    // ===============================

    /**
     * Get a message by key
     */
    public String getMessage(String key) {
        return messageManager.getMessage(key);
    }

    /**
     * Get a message with placeholders
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        return messageManager.getMessage(key, placeholders);
    }

    /**
     * Get a message with single placeholder
     */
    public String getMessage(String key, String placeholder, String value) {
        return messageManager.getMessage(key, placeholder, value);
    }

    /**
     * Send a message to a command sender
     */
    public void sendMessage(CommandSender sender, String key) {
        messageManager.sendMessage(sender, key);
    }

    /**
     * Send a message with placeholders to a command sender
     */
    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        messageManager.sendMessage(sender, key, placeholders);
    }

    /**
     * Send a message with single placeholder to a command sender
     */
    public void sendMessage(CommandSender sender, String key, String placeholder, String value) {
        messageManager.sendMessage(sender, key, placeholder, value);
    }

    /**
     * Send an action bar message to a player
     */
    public void sendActionBar(Player player, String key) {
        messageManager.sendActionBar(player, key);
    }

    /**
     * Send an action bar message with placeholders to a player
     */
    public void sendActionBar(Player player, String key, Map<String, String> placeholders) {
        messageManager.sendActionBar(player, key, placeholders);
    }

    /**
     * Send a title to a player
     */
    public void sendTitle(Player player, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut) {
        messageManager.sendTitle(player, titleKey, subtitleKey, fadeIn, stay, fadeOut);
    }

    /**
     * Send a title with placeholders to a player
     */
    public void sendTitle(Player player, String titleKey, String subtitleKey, Map<String, String> placeholders,
                          int fadeIn, int stay, int fadeOut) {
        messageManager.sendTitle(player, titleKey, subtitleKey, placeholders, fadeIn, stay, fadeOut);
    }

    /**
     * Broadcast a message to all players
     */
    public void broadcast(String key) {
        messageManager.broadcast(key);
    }

    /**
     * Broadcast a message with placeholders to all players
     */
    public void broadcast(String key, Map<String, String> placeholders) {
        messageManager.broadcast(key, placeholders);
    }

    /**
     * Broadcast a message to players with permission
     */
    public void broadcast(String key, String permission) {
        messageManager.broadcast(key, permission);
    }

    /**
     * Colorize a message (supports hex and legacy colors)
     */
    public String colorize(String message) {
        return messageManager.colorize(message);
    }

    /**
     * Strip all colors from a message
     */
    public String stripColors(String message) {
        return messageManager.stripColors(message);
    }

    /**
     * Reload all messages
     */
    public void reloadMessages() {
        messageManager.reload();
    }

    // ===============================
    // PERMISSION MANAGER METHODS
    // ===============================

    /**
     * Add permission to player
     */
    public CompletableFuture<Boolean> addPlayerPermission(UUID uuid, String permission) {
        return permissionManager.addPlayerPermission(uuid, permission);
    }

    /**
     * Remove permission from player
     */
    public CompletableFuture<Boolean> removePlayerPermission(UUID uuid, String permission) {
        return permissionManager.removePlayerPermission(uuid, permission);
    }

    /**
     * Add temporary permission to player
     */
    public CompletableFuture<Boolean> addTempPermission(UUID uuid, String permission, long durationMillis) {
        return permissionManager.addTempPermission(uuid, permission, durationMillis);
    }

    /**
     * Add player to group
     */
    public CompletableFuture<Boolean> addPlayerToGroup(UUID uuid, String groupName) {
        return permissionManager.addPlayerToGroup(uuid, groupName);
    }

    /**
     * Remove player from group
     */
    public CompletableFuture<Boolean> removePlayerFromGroup(UUID uuid, String groupName) {
        return permissionManager.removePlayerFromGroup(uuid, groupName);
    }

    /**
     * Check if player has permission
     */
    public boolean hasPermission(UUID uuid, String permission) {
        return permissionManager.hasPermission(uuid, permission);
    }

    /**
     * Create a new permission group
     */
    public boolean createPermissionGroup(String name, String displayName, int priority) {
        return permissionManager.createGroup(name, displayName, priority);
    }

    /**
     * Delete permission group
     */
    public boolean deletePermissionGroup(String name) {
        return permissionManager.deleteGroup(name);
    }

    /**
     * Get permission group
     */
    public PermissionGroup getPermissionGroup(String name) {
        return permissionManager.getGroup(name);
    }

    /**
     * Get all permission groups
     */
    public java.util.Collection<PermissionGroup> getAllPermissionGroups() {
        return permissionManager.getAllGroups();
    }

    /**
     * Get player's primary group
     */
    public PermissionGroup getPlayerPrimaryGroup(UUID uuid) {
        return permissionManager.getPlayerPrimaryGroup(uuid);
    }

    /**
     * Get player permission data
     */
    public PlayerPermissionData getPlayerPermissionData(UUID uuid) {
        return permissionManager.getPlayerData(uuid);
    }

    /**
     * Apply permissions to player
     */
    public void applyPermissions(Player player) {
        permissionManager.applyPermissions(player);
    }

    // ===============================
    // SECURITY MANAGER METHODS
    // ===============================

    /**
     * Check if player is rate limited
     */
    public boolean isRateLimited(UUID uuid, String action, int maxAttempts, long windowMs) {
        return securityManager.isRateLimited(uuid, action, maxAttempts, windowMs);
    }

    /**
     * Check if IP is rate limited
     */
    public boolean isIPRateLimited(InetAddress ip, String action, int maxAttempts, long windowMs) {
        return securityManager.isIPRateLimited(ip, action, maxAttempts, windowMs);
    }

    /**
     * Reset rate limit for player
     */
    public void resetRateLimit(UUID uuid, String action) {
        securityManager.resetRateLimit(uuid, action);
    }

    /**
     * Check if player has command on cooldown
     */
    public boolean isOnCooldown(UUID uuid, String command) {
        return securityManager.isOnCooldown(uuid, command);
    }

    /**
     * Set command cooldown for player
     */
    public void setCooldown(UUID uuid, String command, long cooldownMs) {
        securityManager.setCooldown(uuid, command, cooldownMs);
    }

    /**
     * Get remaining cooldown time
     */
    public long getRemainingCooldown(UUID uuid, String command) {
        return securityManager.getRemainingCooldown(uuid, command);
    }

    /**
     * Block an IP address
     */
    public CompletableFuture<Boolean> blockIP(InetAddress ip, String reason, UUID blockedBy, Long expiresAt) {
        return securityManager.blockIP(ip, reason, blockedBy, expiresAt);
    }

    /**
     * Unblock an IP address
     */
    public CompletableFuture<Boolean> unblockIP(InetAddress ip) {
        return securityManager.unblockIP(ip);
    }

    /**
     * Check if IP is blocked
     */
    public boolean isIPBlocked(InetAddress ip) {
        return securityManager.isIPBlocked(ip);
    }

    /**
     * Get all blocked IPs
     */
    public Set<InetAddress> getBlockedIPs() {
        return securityManager.getBlockedIPs();
    }

    /**
     * Check if message is spam
     */
    public boolean isSpam(UUID uuid, String message) {
        return securityManager.isSpam(uuid, message);
    }

    /**
     * Record security violation
     */
    public void recordSecurityViolation(InetAddress ip, UUID uuid, SecurityManager.ViolationType type, String details) {
        securityManager.recordViolation(ip, uuid, type, details);
    }

    /**
     * Get security statistics
     */
    public SecurityManager.SecurityStats getSecurityStats() {
        return securityManager.getStats();
    }

    /**
     * Get security configuration
     */
    public de.tecca.eclipse.security.SecurityConfig getSecurityConfig() {
        return securityManager.getConfig();
    }

    // ===============================
    // MIGRATION SYSTEM METHODS
    // ===============================

    /**
     * Run all pending migrations
     */
    public CompletableFuture<MigrationResult> runMigrations() {
        return migrationSystem.runMigrations();
    }

    /**
     * Get migration status
     */
    public MigrationStatus getMigrationStatus() {
        return migrationSystem.getStatus();
    }

    /**
     * Create backup
     */
    public CompletableFuture<String> createBackup() {
        return migrationSystem.createBackup();
    }

    /**
     * Rollback to specific migration
     */
    public CompletableFuture<Boolean> rollbackToMigration(String migrationId) {
        return migrationSystem.rollbackTo(migrationId);
    }

    /**
     * Get available backups
     */
    public java.util.List<String> getAvailableBackups() {
        return migrationSystem.getAvailableBackups();
    }

    /**
     * Restore from backup
     */
    public CompletableFuture<Boolean> restoreFromBackup(String backupName) {
        return migrationSystem.restoreFromBackup(backupName);
    }

    /**
     * Clean up old backups
     */
    public void cleanupOldBackups(int keepCount) {
        migrationSystem.cleanupOldBackups(keepCount);
    }

    /**
     * Register a migration
     */
    public void registerMigration(de.tecca.eclipse.migration.Migration migration) {
        migrationSystem.registerMigration(migration);
    }

    /**
     * Register a config migration
     */
    public void registerConfigMigration(de.tecca.eclipse.migration.ConfigMigration migration) {
        migrationSystem.registerConfigMigration(migration);
    }

    // ===============================
    // MANAGER GETTERS
    // ===============================

    /**
     * Get the plugin instance
     * @return Eclipse plugin instance
     */
    public Eclipse getPlugin() {
        return plugin;
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

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Check if a plugin is using Eclipse as a dependency
     * @param plugin The plugin to check
     * @return true if the plugin depends on Eclipse
     */
    public boolean isDependentPlugin(Plugin plugin) {
        return plugin.getDescription().getDepend().contains("Eclipse") ||
                plugin.getDescription().getSoftDepend().contains("Eclipse");
    }

    /**
     * Get framework version
     * @return Version string
     */
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Get comprehensive framework information
     * @return Information string with all active managers and their status
     */
    public String getFrameworkInfo() {
        return String.format(
                "Eclipse Framework v%s\n" +
                        "├─ Event Manager: %s (%d event types)\n" +
                        "├─ Task Queue: %d pending tasks (%s)\n" +
                        "├─ Database: %s (%s)\n" +
                        "├─ Message Manager: %d loaded messages\n" +
                        "├─ Player Cache: %d cached players\n" +
                        "├─ Permission Manager: %d groups, %d cached players\n" +
                        "├─ Security Manager: %s\n" +
                        "├─ Migration System: %s\n" +
                        "└─ Config Manager: Active",
                getVersion(),
                eventManager != null ? "Active" : "Inactive",
                eventManager != null ? eventManager.getListenerCount(EclipseEvent.class) : 0,
                taskQueue != null ? taskQueue.getQueueSize() : 0,
                taskQueue != null && taskQueue.isRunning() ? "Running" : "Stopped",
                databaseManager != null && databaseManager.isConnected() ?
                        databaseManager.getDatabaseType().toString() : "Disconnected",
                databaseManager != null ? databaseManager.getPoolStats() : "N/A",
                messageManager != null ? messageManager.getAllMessages().size() : 0,
                playerCache != null ? playerCache.getCacheSize() : 0,
                permissionManager != null ? permissionManager.getAllGroups().size() : 0,
                permissionManager != null ? "Active" : "Inactive",
                securityManager != null ? securityManager.getStats().toString() : "Inactive",
                migrationSystem != null ? migrationSystem.getStatus().toString() : "Inactive"
        );
    }

    /**
     * Get simple framework status
     * @return Simple status string
     */
    public String getStatus() {
        return String.format("Eclipse v%s - DB: %s, Cache: %d, Queue: %d, Security: %s",
                getVersion(),
                isDatabaseConnected() ? getDatabaseType() : "Disconnected",
                playerCache.getCacheSize(),
                getQueueSize(),
                securityManager != null ? securityManager.getStats().getBlockedIPs() + " blocked IPs" : "Inactive");
    }

    /**
     * Perform a complete framework reload
     */
    public void reloadFramework() {
        plugin.getLogger().info("Reloading Eclipse Framework...");

        try {
            // Reload configurations
            configManager.reloadAll();
            plugin.getLogger().info("✓ Configurations reloaded");

            // Reload messages
            messageManager.reload();
            plugin.getLogger().info("✓ Messages reloaded");

            // Clean up expired data
            if (playerCache != null) {
                playerCache.cleanupExpiredData();
                plugin.getLogger().info("✓ Player cache cleaned");
            }

            plugin.getLogger().info("Eclipse Framework reload completed successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Error during framework reload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shutdown all framework components gracefully
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Eclipse Framework via API...");

        // This will trigger the main plugin's onDisable method
        plugin.getServer().getPluginManager().disablePlugin(plugin);
    }

    /**
     * Perform comprehensive health check
     */
    public HealthCheckResult performHealthCheck() {
        HealthCheckResult result = new HealthCheckResult();

        // Check database
        result.addCheck("Database", databaseManager != null && databaseManager.isConnected());

        // Check task queue
        result.addCheck("Task Queue", taskQueue != null && taskQueue.isRunning());

        // Check event manager
        result.addCheck("Event Manager", eventManager != null);

        // Check player cache
        result.addCheck("Player Cache", playerCache != null);

        // Check permission manager
        result.addCheck("Permission Manager", permissionManager != null);

        // Check security manager
        result.addCheck("Security Manager", securityManager != null);

        // Check migration system
        result.addCheck("Migration System", migrationSystem != null);

        // Check message manager
        result.addCheck("Message Manager", messageManager != null);

        // Check config manager
        result.addCheck("Config Manager", configManager != null);

        return result;
    }

    /**
     * Health check result
     */
    public static class HealthCheckResult {
        private final Map<String, Boolean> checks = new java.util.HashMap<>();
        private final long timestamp = System.currentTimeMillis();

        public void addCheck(String component, boolean healthy) {
            checks.put(component, healthy);
        }

        public boolean isHealthy() {
            return checks.values().stream().allMatch(Boolean::booleanValue);
        }

        public Map<String, Boolean> getChecks() {
            return new java.util.HashMap<>(checks);
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getHealthyCount() {
            return (int) checks.values().stream().mapToLong(b -> b ? 1 : 0).sum();
        }

        public int getTotalCount() {
            return checks.size();
        }

        @Override
        public String toString() {
            return String.format("HealthCheck{healthy=%d/%d, overall=%s, timestamp=%d}",
                    getHealthyCount(), getTotalCount(), isHealthy() ? "HEALTHY" : "UNHEALTHY", timestamp);
        }
    }
}