package de.tecca.eclipse.permission;

import java.util.*;

/**
 * Represents a permission group with inheritance support
 */
public class PermissionGroup {

    private final String name;
    private String displayName;
    private int priority;
    private final Set<String> permissions;
    private final Set<String> inherits;
    private String prefix;
    private String suffix;

    public PermissionGroup(String name, String displayName, int priority) {
        this.name = name;
        this.displayName = displayName;
        this.priority = priority;
        this.permissions = new HashSet<>();
        this.inherits = new HashSet<>();
        this.prefix = "";
        this.suffix = "";
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Set<String> getPermissions() {
        return new HashSet<>(permissions);
    }

    public void addPermission(String permission) {
        permissions.add(permission);
    }

    public void removePermission(String permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(String permission) {
        // Check wildcard permissions
        if (permissions.contains("*")) return true;

        // Check exact permission
        if (permissions.contains(permission)) return true;

        // Check wildcard matches
        for (String perm : permissions) {
            if (perm.endsWith("*")) {
                String prefix = perm.substring(0, perm.length() - 1);
                if (permission.startsWith(prefix)) return true;
            }
        }

        return false;
    }

    public Set<String> getInherits() {
        return new HashSet<>(inherits);
    }

    public void addInheritance(String groupName) {
        inherits.add(groupName);
    }

    public void removeInheritance(String groupName) {
        inherits.remove(groupName);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix != null ? suffix : "";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PermissionGroup that = (PermissionGroup) obj;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return String.format("PermissionGroup{name='%s', displayName='%s', priority=%d, permissions=%d}",
                name, displayName, priority, permissions.size());
    }
}