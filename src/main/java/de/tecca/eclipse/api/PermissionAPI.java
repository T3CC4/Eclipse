package de.tecca.eclipse.api;

import de.tecca.eclipse.api.permissions.*;
import java.util.UUID;
import java.util.List;
import java.util.Map;

public interface PermissionAPI {

    boolean hasPermission(UUID playerUuid, String permission);
    void addPermission(UUID playerUuid, String permission);
    void removePermission(UUID playerUuid, String permission);
    void addTemporaryPermission(UUID playerUuid, String permission, long durationSeconds);
    List<String> getPlayerPermissions(UUID playerUuid);

    void addPlayerToGroup(UUID playerUuid, String groupName);
    void removePlayerFromGroup(UUID playerUuid, String groupName);
    List<String> getPlayerGroups(UUID playerUuid);

    void createGroup(String groupName, String displayName, int priority);
    void deleteGroup(String groupName);
    void addGroupPermission(String groupName, String permission);
    void removeGroupPermission(String groupName, String permission);
    void setGroupParent(String groupName, String parentGroup);
    List<String> getGroupPermissions(String groupName);

    Map<String, Object> getPlayerData(UUID playerUuid);
    List<PermissionGroup> getAllGroups();
    boolean groupExists(String groupName);

    void shutdown();
}