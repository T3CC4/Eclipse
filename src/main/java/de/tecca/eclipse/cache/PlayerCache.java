package de.tecca.eclipse.cache;

import de.tecca.eclipse.Eclipse;
import de.tecca.eclipse.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Professional player data caching system
 */
public class PlayerCache implements Listener {

    private final Eclipse plugin;
    private final Map<UUID, PlayerData> cache;
    private final Map<String, Function<UUID, CompletableFuture<Object>>> dataLoaders;

    public PlayerCache(Eclipse plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.dataLoaders = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start cleanup task
        startCleanupTask();
    }

    /**
     * Start periodic cleanup task
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                this::cleanupExpiredData, 20L * 300, 20L * 300); // Every 5 minutes
    }

    /**
     * Register a data loader for a specific key
     * @param key The data key
     * @param loader Function to load data for a UUID
     */
    public void registerDataLoader(String key, Function<UUID, CompletableFuture<Object>> loader) {
        dataLoaders.put(key, loader);
    }

    /**
     * Get player data from cache
     * @param uuid Player UUID
     * @return PlayerData or null if not cached
     */
    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Get player data from cache by player
     * @param player The player
     * @return PlayerData or null if not cached
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    /**
     * Load player data into cache - COMPLETE IMPLEMENTATION
     * @param uuid Player UUID
     * @return CompletableFuture with PlayerData
     */
    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseManager database = plugin.getDatabaseManager();
                PlayerData data;

                if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
                    data = loadPlayerDataJSON(uuid, database);
                } else {
                    data = loadPlayerDataSQL(uuid, database);
                }

                // If no data found, create new player data
                if (data == null) {
                    data = new PlayerData(uuid);
                    plugin.getLogger().fine("Created new player data for " + uuid);
                } else {
                    plugin.getLogger().fine("Loaded existing player data for " + uuid);
                }

                // Load additional data using registered data loaders
                loadAdditionalData(uuid, data);

                // Cache the loaded data
                cache.put(uuid, data);

                return data;

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
                e.printStackTrace();

                // Return empty player data on error to prevent null issues
                PlayerData fallbackData = new PlayerData(uuid);
                cache.put(uuid, fallbackData);
                return fallbackData;
            }
        });
    }

    /**
     * Load player data from JSON storage
     */
    private PlayerData loadPlayerDataJSON(UUID uuid, DatabaseManager database) {
        Map<String, Object> playerDataMap = database.getJSONData("player_data_" + uuid.toString(), Map.class);

        if (playerDataMap == null) {
            return null;
        }

        PlayerData data = new PlayerData(uuid);

        // Load saved data
        Object savedData = playerDataMap.get("data");
        if (savedData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) savedData;

            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                data.setData(entry.getKey(), entry.getValue());
            }
        }

        // Data is not dirty after loading
        data.markSaved();

        return data;
    }

    /**
     * Load player data from SQL database
     */
    private PlayerData loadPlayerDataSQL(UUID uuid, DatabaseManager database) {
        return database.executeQueryWithResult(
                "SELECT data_json, load_time FROM eclipse_player_data WHERE uuid = ?",
                rs -> {
                    try {
                        if (rs.next()) {
                            PlayerData data = new PlayerData(uuid);

                            String dataJson = rs.getString("data_json");
                            if (dataJson != null && !dataJson.isEmpty()) {
                                Map<String, Object> dataMap = deserializeDataFromJSON(dataJson);

                                for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                                    data.setData(entry.getKey(), entry.getValue());
                                }
                            }

                            // Data is not dirty after loading
                            data.markSaved();

                            return data;
                        }
                        return null;
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error parsing player data from database: " + e.getMessage());
                        return null;
                    }
                },
                uuid.toString()
        );
    }

    /**
     * Load additional data using registered data loaders
     */
    private void loadAdditionalData(UUID uuid, PlayerData data) {
        for (Map.Entry<String, Function<UUID, CompletableFuture<Object>>> entry : dataLoaders.entrySet()) {
            try {
                Object value = entry.getValue().apply(uuid).get(5, java.util.concurrent.TimeUnit.SECONDS);
                if (value != null) {
                    data.setData(entry.getKey(), value);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load additional data '" + entry.getKey() + "' for " + uuid + ": " + e.getMessage());
            }
        }

        // Mark as saved since additional data loaders don't make it dirty
        data.markSaved();
    }

    /**
     * Save player data - COMPLETE IMPLEMENTATION
     * @param uuid Player UUID
     * @return CompletableFuture for save completion
     */
    public CompletableFuture<Void> savePlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Don't save if data hasn't been modified
        if (!data.isDirty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager database = plugin.getDatabaseManager();

                if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
                    // Save to JSON storage
                    savePlayerDataJSON(uuid, data, database);
                } else {
                    // Save to SQL database (MySQL/SQLite)
                    savePlayerDataSQL(uuid, data, database);
                }

                // Mark as saved after successful save
                data.markSaved();

                plugin.getLogger().fine("Saved player data for " + uuid);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Player data save failed", e);
            }
        });
    }

    /**
     * Save player data to JSON storage
     */
    private void savePlayerDataJSON(UUID uuid, PlayerData data, DatabaseManager database) {
        // Create a serializable data structure
        Map<String, Object> playerDataMap = new HashMap<>();
        playerDataMap.put("uuid", uuid.toString());
        playerDataMap.put("loadTime", data.getLoadTime());
        playerDataMap.put("lastSaved", System.currentTimeMillis());

        // Save all player data
        Map<String, Object> allData = data.getAllData();
        Map<String, Object> serializedData = new HashMap<>();

        for (Map.Entry<String, Object> entry : allData.entrySet()) {
            Object value = entry.getValue();

            // Handle different data types for JSON serialization
            if (value instanceof java.io.Serializable) {
                serializedData.put(entry.getKey(), value);
            } else {
                // Convert complex objects to strings or maps
                serializedData.put(entry.getKey(), value.toString());
            }
        }

        playerDataMap.put("data", serializedData);

        // Save to JSON with player UUID as key
        database.setJSONData("player_data_" + uuid.toString(), playerDataMap);
    }

    /**
     * Save player data to SQL database
     */
    private void savePlayerDataSQL(UUID uuid, PlayerData data, DatabaseManager database) {
        // Ensure player_data table exists
        database.createTableIfNotExists("eclipse_player_data",
                "(uuid VARCHAR(36) PRIMARY KEY, " +
                        "data_json TEXT, " +
                        "load_time BIGINT, " +
                        "last_saved BIGINT DEFAULT 0, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

        // Convert player data to JSON string for storage
        Map<String, Object> allData = data.getAllData();
        String dataJson = serializeDataToJSON(allData);

        // Insert or update player data
        database.executeUpdate(
                "INSERT INTO eclipse_player_data (uuid, data_json, load_time, last_saved) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "data_json = VALUES(data_json), " +
                        "last_saved = VALUES(last_saved), " +
                        "updated_at = CURRENT_TIMESTAMP",
                uuid.toString(),
                dataJson,
                data.getLoadTime(),
                System.currentTimeMillis()
        );
    }

    /**
     * Serialize data to JSON string
     */
    private String serializeDataToJSON(Map<String, Object> data) {
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            return gson.toJson(data);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to serialize player data to JSON: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * Deserialize data from JSON string
     */
    private Map<String, Object> deserializeDataFromJSON(String json) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> result = gson.fromJson(json, type);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize player data from JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Remove player data from cache - COMPLETE IMPLEMENTATION
     * @param uuid Player UUID
     * @param save Whether to save before removing
     */
    public CompletableFuture<Void> removePlayerData(UUID uuid, boolean save) {
        if (save) {
            return savePlayerData(uuid).thenRun(() -> {
                PlayerData removed = cache.remove(uuid);
                if (removed != null) {
                    plugin.getLogger().fine("Removed player data from cache (saved): " + uuid);
                }
            }).exceptionally(throwable -> {
                plugin.getLogger().warning("Failed to save player data before removal: " + throwable.getMessage());
                // Remove anyway to prevent memory leaks
                cache.remove(uuid);
                return null;
            });
        } else {
            PlayerData removed = cache.remove(uuid);
            if (removed != null) {
                plugin.getLogger().fine("Removed player data from cache (unsaved): " + uuid);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Check if player data is cached
     * @param uuid Player UUID
     * @return true if cached
     */
    public boolean isCached(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /**
     * Get cache size
     * @return Number of cached players
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Clear all cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Save all cached data - COMPLETE IMPLEMENTATION
     */
    public CompletableFuture<Void> saveAll() {
        plugin.getLogger().info("Saving all player cache data...");

        List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
        int totalPlayers = cache.size();
        int dirtyPlayers = 0;

        // Create save futures for all cached players
        for (UUID uuid : cache.keySet()) {
            PlayerData data = cache.get(uuid);
            if (data != null && data.isDirty()) {
                dirtyPlayers++;
                CompletableFuture<Void> saveFuture = savePlayerData(uuid)
                        .exceptionally(throwable -> {
                            plugin.getLogger().warning("Failed to save data for player " + uuid + ": " + throwable.getMessage());
                            return null;
                        });
                saveFutures.add(saveFuture);
            }
        }

        final int finalDirtyPlayers = dirtyPlayers;

        // Wait for all saves to complete
        return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    plugin.getLogger().info("Player cache save complete - " +
                            finalDirtyPlayers + "/" + totalPlayers + " players saved (dirty data only)");
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error during player cache save: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Clear expired data from cache
     */
    public void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        int removed = 0;

        Iterator<Map.Entry<UUID, PlayerData>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerData> entry = iterator.next();
            PlayerData data = entry.getValue();

            // Remove data older than 1 hour for offline players
            if (currentTime - data.getLoadTime() > 3600000) { // 1 hour
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    // Save before removing if dirty
                    if (data.isDirty()) {
                        try {
                            savePlayerData(entry.getKey()).get(5, java.util.concurrent.TimeUnit.SECONDS);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to save expired player data for " + entry.getKey() + ": " + e.getMessage());
                        }
                    }
                    iterator.remove();
                    removed++;
                }
            }
        }

        if (removed > 0) {
            plugin.getLogger().info("Cleaned up " + removed + " expired player data entries");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer().getUniqueId()).thenAccept(data -> {
            plugin.getLogger().fine("Player data loaded for " + event.getPlayer().getName());
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to load player data on join for " + event.getPlayer().getName() + ": " + throwable.getMessage());
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerData(event.getPlayer().getUniqueId(), true).thenRun(() -> {
            plugin.getLogger().fine("Player data saved and removed for " + event.getPlayer().getName());
        }).exceptionally(throwable -> {
            plugin.getLogger().warning("Failed to save player data on quit for " + event.getPlayer().getName() + ": " + throwable.getMessage());
            return null;
        });
    }

    /**
     * Shutdown player cache - COMPLETE IMPLEMENTATION
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Player Cache...");

        // Save all data synchronously with timeout
        try {
            saveAll().get(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().severe("Timeout or error saving player data during shutdown: " + e.getMessage());
        }

        // Clear data loaders
        int loaderCount = dataLoaders.size();
        dataLoaders.clear();

        // Clear cached data
        int cacheCount = cache.size();
        cache.clear();

        // Cancel cleanup task
        plugin.getServer().getScheduler().cancelTasks(plugin);

        plugin.getLogger().info("Player Cache shutdown complete - " + cacheCount + " players cleared, " + loaderCount + " loaders cleared");
    }

    /**
     * Player data container
     */
    public static class PlayerData {
        private final UUID uuid;
        private final Map<String, Object> data;
        private final long loadTime;
        private long lastSaved;
        private boolean dirty = false;

        public PlayerData(UUID uuid) {
            this.uuid = uuid;
            this.data = new ConcurrentHashMap<>();
            this.loadTime = System.currentTimeMillis();
            this.lastSaved = loadTime;
        }

        public UUID getUUID() {
            return uuid;
        }

        public <T> Optional<T> getData(String key, Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            return Optional.empty();
        }

        public <T> T getData(String key, Class<T> type, T defaultValue) {
            return getData(key, type).orElse(defaultValue);
        }

        public void setData(String key, Object value) {
            data.put(key, value);
            this.dirty = true;
        }

        public boolean hasData(String key) {
            return data.containsKey(key);
        }

        public void removeData(String key) {
            data.remove(key);
            this.dirty = true;
        }

        public boolean isDirty() {
            return dirty;
        }

        public void markSaved() {
            this.dirty = false;
            this.lastSaved = System.currentTimeMillis();
        }

        public long getLoadTime() {
            return loadTime;
        }

        public long getLastSaved() {
            return lastSaved;
        }

        public Map<String, Object> getAllData() {
            return new ConcurrentHashMap<>(data);
        }
    }
}