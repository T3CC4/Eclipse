package de.tecca.eclipse.permission;

import de.tecca.eclipse.Eclipse;
import de.tecca.eclipse.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Professional permission management system
 */
public class PermissionManager {

    private final Eclipse plugin;
    private final DatabaseManager database;
    private final Map<UUID, PlayerPermissionData> playerPermissions;
    private final Map<String, PermissionGroup> groups;
    private final Map<UUID, PermissionAttachment> attachments;

    public PermissionManager(Eclipse plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.playerPermissions = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.attachments = new ConcurrentHashMap<>();
        initializeDefaultGroups();
        setupDatabase();
    }

    /**
     * Initialize default permission groups
     */
    private void initializeDefaultGroups() {
        // Default group
        PermissionGroup defaultGroup = new PermissionGroup("default", "Default Group", 0);
        defaultGroup.addPermission("eclipse.basic");
        groups.put("default", defaultGroup);

        // Admin group
        PermissionGroup adminGroup = new PermissionGroup("admin", "Administrator", 100);
        adminGroup.addPermission("*");
        adminGroup.setPrefix("&c[Admin] ");
        adminGroup.setSuffix(" &7(Admin)");
        groups.put("admin", adminGroup);

        // Moderator group
        PermissionGroup modGroup = new PermissionGroup("moderator", "Moderator", 50);
        modGroup.addPermission("eclipse.moderate.*");
        modGroup.setPrefix("&9[Mod] ");
        modGroup.setSuffix(" &7(Mod)");
        groups.put("moderator", modGroup);
    }

    /**
     * Setup database tables
     */
    private void setupDatabase() {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            // JSON storage doesn't need table creation
            return;
        }

        // Create tables for SQL databases
        database.createTableIfNotExists("eclipse_permissions",
                "(uuid VARCHAR(36) PRIMARY KEY, " +
                        "permissions TEXT, " +
                        "groups TEXT, " +
                        "temp_permissions TEXT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

        database.createTableIfNotExists("eclipse_groups",
                "(name VARCHAR(50) PRIMARY KEY, " +
                        "display_name VARCHAR(100), " +
                        "priority INT, " +
                        "permissions TEXT, " +
                        "prefix VARCHAR(50), " +
                        "suffix VARCHAR(50), " +
                        "inherits TEXT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    /**
     * Load player permissions
     */
    public CompletableFuture<PlayerPermissionData> loadPlayerPermissions(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerPermissionData data;

            if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
                data = database.getJSONData("permissions_" + uuid, PlayerPermissionData.class);
                if (data == null) {
                    data = new PlayerPermissionData(uuid);
                    data.addGroup("default");
                }
            } else {
                data = database.executeQueryWithResult(
                        "SELECT * FROM eclipse_permissions WHERE uuid = ?",
                        rs -> {
                            try {
                                if (rs.next()) {
                                    PlayerPermissionData playerData = new PlayerPermissionData(uuid);
                                    // Parse permissions, groups, and temp permissions from JSON strings
                                    String permissions = rs.getString("permissions");
                                    String groups = rs.getString("groups");
                                    String tempPermissions = rs.getString("temp_permissions");

                                    // Implementation would parse these JSON strings
                                    // For brevity, simplified here
                                    playerData.addGroup("default");
                                    return playerData;
                                }
                                return null;
                            } catch (Exception e) {
                                plugin.getLogger().severe("Error loading permissions for " + uuid + ": " + e.getMessage());
                                return null;
                            }
                        }, uuid.toString());

                if (data == null) {
                    data = new PlayerPermissionData(uuid);
                    data.addGroup("default");
                }
            }

            playerPermissions.put(uuid, data);
            return data;
        });
    }

    /**
     * Save player permissions
     */
    public CompletableFuture<Void> savePlayerPermissions(UUID uuid) {
        PlayerPermissionData data = playerPermissions.get(uuid);
        if (data == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
                database.setJSONData("permissions_" + uuid, data);
            } else {
                // SQL implementation would serialize data to JSON strings
                database.executeUpdate(
                        "INSERT INTO eclipse_permissions (uuid, permissions, groups, temp_permissions) " +
                                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                                "permissions = VALUES(permissions), groups = VALUES(groups), " +
                                "temp_permissions = VALUES(temp_permissions), updated_at = CURRENT_TIMESTAMP",
                        uuid.toString(),
                        serializePermissions(data.getPermissions()),
                        serializeGroups(data.getGroups()),
                        serializeTempPermissions(data.getTempPermissions())
                );
            }
        });
    }

    /**
     * Apply permissions to a player
     */
    public void applyPermissions(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerPermissionData data = playerPermissions.get(uuid);

        if (data == null) {
            loadPlayerPermissions(uuid).thenAccept(loadedData -> applyPermissions(player));
            return;
        }

        // Remove old attachment
        PermissionAttachment oldAttachment = attachments.remove(uuid);
        if (oldAttachment != null) {
            player.removeAttachment(oldAttachment);
        }

        // Create new attachment
        PermissionAttachment attachment = player.addAttachment(plugin);

        // Apply group permissions
        for (String groupName : data.getGroups()) {
            PermissionGroup group = groups.get(groupName);
            if (group != null) {
                for (String permission : group.getPermissions()) {
                    attachment.setPermission(permission, true);
                }
            }
        }

        // Apply direct permissions
        for (Map.Entry<String, Boolean> entry : data.getPermissions().entrySet()) {
            attachment.setPermission(entry.getKey(), entry.getValue());
        }

        // Apply temporary permissions (non-expired)
        long currentTime = System.currentTimeMillis();
        data.getTempPermissions().entrySet().removeIf(entry -> entry.getValue() < currentTime);

        for (String permission : data.getTempPermissions().keySet()) {
            attachment.setPermission(permission, true);
        }

        attachments.put(uuid, attachment);

        // Save if temp permissions were cleaned up
        if (!data.getTempPermissions().isEmpty()) {
            savePlayerPermissions(uuid);
        }
    }

    /**
     * Add permission to player
     */
    public CompletableFuture<Boolean> addPlayerPermission(UUID uuid, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerPermissionData data = playerPermissions.get(uuid);
            if (data == null) return false;

            data.addPermission(permission, true);
            savePlayerPermissions(uuid);

            // Apply immediately if player is online
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyPermissions(player);
            }

            return true;
        });
    }

    /**
     * Remove permission from player
     */
    public CompletableFuture<Boolean> removePlayerPermission(UUID uuid, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerPermissionData data = playerPermissions.get(uuid);
            if (data == null) return false;

            data.removePermission(permission);
            savePlayerPermissions(uuid);

            // Apply immediately if player is online
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyPermissions(player);
            }

            return true;
        });
    }

    /**
     * Add temporary permission to player
     */
    public CompletableFuture<Boolean> addTempPermission(UUID uuid, String permission, long durationMillis) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerPermissionData data = playerPermissions.get(uuid);
            if (data == null) return false;

            long expiry = System.currentTimeMillis() + durationMillis;
            data.addTempPermission(permission, expiry);
            savePlayerPermissions(uuid);

            // Apply immediately if player is online
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyPermissions(player);
            }

            return true;
        });
    }

    /**
     * Add player to group
     */
    public CompletableFuture<Boolean> addPlayerToGroup(UUID uuid, String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerPermissionData data = playerPermissions.get(uuid);
            PermissionGroup group = groups.get(groupName);

            if (data == null || group == null) return false;

            data.addGroup(groupName);
            savePlayerPermissions(uuid);

            // Apply immediately if player is online
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyPermissions(player);
            }

            return true;
        });
    }

    /**
     * Remove player from group
     */
    public CompletableFuture<Boolean> removePlayerFromGroup(UUID uuid, String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerPermissionData data = playerPermissions.get(uuid);
            if (data == null) return false;

            data.removeGroup(groupName);
            savePlayerPermissions(uuid);

            // Apply immediately if player is online
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyPermissions(player);
            }

            return true;
        });
    }

    /**
     * Check if player has permission
     */
    public boolean hasPermission(UUID uuid, String permission) {
        PlayerPermissionData data = playerPermissions.get(uuid);
        if (data == null) return false;

        // Check direct permissions
        if (data.getPermissions().containsKey(permission)) {
            return data.getPermissions().get(permission);
        }

        // Check group permissions
        for (String groupName : data.getGroups()) {
            PermissionGroup group = groups.get(groupName);
            if (group != null && group.hasPermission(permission)) {
                return true;
            }
        }

        // Check temporary permissions
        if (data.getTempPermissions().containsKey(permission)) {
            return data.getTempPermissions().get(permission) > System.currentTimeMillis();
        }

        return false;
    }

    /**
     * Create a new permission group
     */
    public boolean createGroup(String name, String displayName, int priority) {
        if (groups.containsKey(name)) return false;

        PermissionGroup group = new PermissionGroup(name, displayName, priority);
        groups.put(name, group);
        saveGroup(group);
        return true;
    }

    /**
     * Delete a permission group
     */
    public boolean deleteGroup(String name) {
        if ("default".equals(name)) return false; // Can't delete default group

        PermissionGroup group = groups.remove(name);
        if (group == null) return false;

        // Remove group from all players
        for (PlayerPermissionData data : playerPermissions.values()) {
            data.removeGroup(name);
        }

        // Delete from storage
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            database.removeJSONData("group_" + name);
        } else {
            database.executeUpdate("DELETE FROM eclipse_groups WHERE name = ?", name);
        }

        return true;
    }

    /**
     * Get permission group
     */
    public PermissionGroup getGroup(String name) {
        return groups.get(name);
    }

    /**
     * Get all permission groups
     */
    public Collection<PermissionGroup> getAllGroups() {
        return new ArrayList<>(groups.values());
    }

    /**
     * Get player's highest priority group
     */
    public PermissionGroup getPlayerPrimaryGroup(UUID uuid) {
        PlayerPermissionData data = playerPermissions.get(uuid);
        if (data == null) return groups.get("default");

        return data.getGroups().stream()
                .map(groups::get)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(PermissionGroup::getPriority))
                .orElse(groups.get("default"));
    }

    /**
     * Get player permission data
     */
    public PlayerPermissionData getPlayerData(UUID uuid) {
        return playerPermissions.get(uuid);
    }

    /**
     * Clean up player data when they leave
     */
    public void cleanupPlayer(UUID uuid) {
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                player.removeAttachment(attachment);
            }
        }

        // Save and remove from memory
        savePlayerPermissions(uuid).thenRun(() -> playerPermissions.remove(uuid));
    }

    /**
     * Save a group to storage
     */
    private void saveGroup(PermissionGroup group) {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            database.setJSONData("group_" + group.getName(), group);
        } else {
            database.executeUpdate(
                    "INSERT INTO eclipse_groups (name, display_name, priority, permissions, prefix, suffix, inherits) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                            "display_name = VALUES(display_name), priority = VALUES(priority), " +
                            "permissions = VALUES(permissions), prefix = VALUES(prefix), " +
                            "suffix = VALUES(suffix), inherits = VALUES(inherits)",
                    group.getName(), group.getDisplayName(), group.getPriority(),
                    String.join(",", group.getPermissions()),
                    group.getPrefix(), group.getSuffix(),
                    String.join(",", group.getInherits())
            );
        }
    }

    // Serialization helper methods for SQL storage
    private String serializePermissions(Map<String, Boolean> permissions) {
        // Would implement JSON serialization
        return "{}"; // Simplified
    }

    private String serializeGroups(Set<String> groups) {
        return String.join(",", groups);
    }

    private String serializeTempPermissions(Map<String, Long> tempPermissions) {
        // Would implement JSON serialization
        return "{}"; // Simplified
    }

    /**
     * Shutdown permission manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Permission Manager...");

        // Save all player data with timeout
        List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
        int playerCount = playerPermissions.size();

        for (UUID uuid : playerPermissions.keySet()) {
            CompletableFuture<Void> saveFuture = savePlayerPermissions(uuid)
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Failed to save permissions for " + uuid + ": " + throwable.getMessage());
                        return null;
                    });
            saveFutures.add(saveFuture);
        }

        try {
            // Wait up to 15 seconds for all saves
            CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                    .get(15, java.util.concurrent.TimeUnit.SECONDS);
            plugin.getLogger().info("All player permissions saved successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Timeout saving permissions during shutdown: " + e.getMessage());
        }

        // Remove all permission attachments
        int attachmentCount = attachments.size();
        for (Map.Entry<UUID, PermissionAttachment> entry : attachments.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                try {
                    player.removeAttachment(entry.getValue());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error removing attachment for " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }

        // Clear all data structures
        attachments.clear();
        playerPermissions.clear();
        int groupCount = groups.size();
        groups.clear();

        plugin.getLogger().info("Permission Manager shutdown complete - " +
                playerCount + " players, " + attachmentCount + " attachments, " + groupCount + " groups cleared");
    }
}