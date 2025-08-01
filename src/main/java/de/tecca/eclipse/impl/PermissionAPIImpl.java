package de.tecca.eclipse.impl;

import de.tecca.eclipse.api.PermissionAPI;
import de.tecca.eclipse.api.permissions.*;
import de.tecca.eclipse.permissions.*;
import org.bukkit.plugin.Plugin;
import java.util.UUID;
import java.util.List;
import java.util.Map;

public class PermissionAPIImpl implements PermissionAPI {

    private final Plugin plugin;
    private final PermissionManager permissionManager;
    private final GroupManager groupManager;

    public PermissionAPIImpl(Plugin plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
        this.groupManager = new GroupManager(plugin);
    }

    @Override
    public boolean hasPermission(UUID playerUuid, String permission) {
        return permissionManager.hasPermission(playerUuid, permission);
    }

    @Override
    public void addPermission(UUID playerUuid, String permission) {
        permissionManager.addPermission(playerUuid, permission);
    }

    @Override
    public void removePermission(UUID playerUuid, String permission) {
        permissionManager.removePermission(playerUuid, permission);
    }

    @Override
    public void addTemporaryPermission(UUID playerUuid, String permission, long durationSeconds) {
        permissionManager.addTemporaryPermission(playerUuid, permission, durationSeconds);
    }

    @Override
    public List<String> getPlayerPermissions(UUID playerUuid) {
        return permissionManager.getPlayerPermissions(playerUuid);
    }

    @Override
    public void addPlayerToGroup(UUID playerUuid, String groupName) {
        groupManager.addPlayerToGroup(playerUuid, groupName);
    }

    @Override
    public void removePlayerFromGroup(UUID playerUuid, String groupName) {
        groupManager.removePlayerFromGroup(playerUuid, groupName);
    }

    @Override
    public List<String> getPlayerGroups(UUID playerUuid) {
        return groupManager.getPlayerGroups(playerUuid);
    }

    @Override
    public void createGroup(String groupName, String displayName, int priority) {
        groupManager.createGroup(groupName, displayName, priority);
    }

    @Override
    public void deleteGroup(String groupName) {
        groupManager.deleteGroup(groupName);
    }

    @Override
    public void addGroupPermission(String groupName, String permission) {
        groupManager.addGroupPermission(groupName, permission);
    }

    @Override
    public void removeGroupPermission(String groupName, String permission) {
        groupManager.removeGroupPermission(groupName, permission);
    }

    @Override
    public void setGroupParent(String groupName, String parentGroup) {
        groupManager.setGroupParent(groupName, parentGroup);
    }

    @Override
    public List<String> getGroupPermissions(String groupName) {
        return groupManager.getGroupPermissions(groupName);
    }

    @Override
    public Map<String, Object> getPlayerData(UUID playerUuid) {
        return permissionManager.getPlayerData(playerUuid);
    }

    @Override
    public List<PermissionGroup> getAllGroups() {
        return groupManager.getAllGroups();
    }

    @Override
    public boolean groupExists(String groupName) {
        return groupManager.groupExists(groupName);
    }

    @Override
    public void shutdown() {
        permissionManager.shutdown();
        groupManager.shutdown();
    }
}