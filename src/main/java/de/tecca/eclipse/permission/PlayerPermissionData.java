package de.tecca.eclipse.permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores permission data for a specific player
 */
public class PlayerPermissionData {

    private final UUID uuid;
    private final Map<String, Boolean> permissions;
    private final Set<String> groups;
    private final Map<String, Long> tempPermissions; // permission -> expiry time
    private long lastUpdated;

    public PlayerPermissionData(UUID uuid) {
        this.uuid = uuid;
        this.permissions = new ConcurrentHashMap<>();
        this.groups = ConcurrentHashMap.newKeySet();
        this.tempPermissions = new ConcurrentHashMap<>();
        this.lastUpdated = System.currentTimeMillis();
    }

    public UUID getUUID() {
        return uuid;
    }

    public Map<String, Boolean> getPermissions() {
        return new HashMap<>(permissions);
    }

    public void addPermission(String permission, boolean value) {
        permissions.put(permission, value);
        updateTimestamp();
    }

    public void removePermission(String permission) {
        permissions.remove(permission);
        updateTimestamp();
    }

    public boolean hasDirectPermission(String permission) {
        return permissions.containsKey(permission) && permissions.get(permission);
    }

    public Set<String> getGroups() {
        return new HashSet<>(groups);
    }

    public void addGroup(String groupName) {
        groups.add(groupName);
        updateTimestamp();
    }

    public void removeGroup(String groupName) {
        groups.remove(groupName);
        updateTimestamp();
    }

    public boolean isInGroup(String groupName) {
        return groups.contains(groupName);
    }

    public Map<String, Long> getTempPermissions() {
        return new HashMap<>(tempPermissions);
    }

    public void addTempPermission(String permission, long expiryTime) {
        tempPermissions.put(permission, expiryTime);
        updateTimestamp();
    }

    public void removeTempPermission(String permission) {
        tempPermissions.remove(permission);
        updateTimestamp();
    }

    public boolean hasTempPermission(String permission) {
        Long expiry = tempPermissions.get(permission);
        if (expiry == null) return false;

        if (expiry < System.currentTimeMillis()) {
            tempPermissions.remove(permission);
            return false;
        }

        return true;
    }

    public void cleanExpiredTempPermissions() {
        long currentTime = System.currentTimeMillis();
        tempPermissions.entrySet().removeIf(entry -> entry.getValue() < currentTime);
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    private void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Get all effective permissions (direct + temporary)
     */
    public Set<String> getAllEffectivePermissions() {
        Set<String> effective = new HashSet<>();

        // Add direct permissions that are true
        permissions.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .forEach(effective::add);

        // Add non-expired temporary permissions
        long currentTime = System.currentTimeMillis();
        tempPermissions.entrySet().stream()
                .filter(entry -> entry.getValue() > currentTime)
                .map(Map.Entry::getKey)
                .forEach(effective::add);

        return effective;
    }

    /**
     * Clear all permissions and groups
     */
    public void reset() {
        permissions.clear();
        groups.clear();
        tempPermissions.clear();
        groups.add("default"); // Always have default group
        updateTimestamp();
    }

    @Override
    public String toString() {
        return String.format("PlayerPermissionData{uuid=%s, permissions=%d, groups=%s, tempPermissions=%d}",
                uuid, permissions.size(), groups, tempPermissions.size());
    }
}