package de.tecca.eclipse.api.permissions;

import java.util.List;

public class PermissionGroup {
    private final String name;
    private final String displayName;
    private final int priority;
    private final List<String> permissions;
    private final String parentGroup;

    public PermissionGroup(String name, String displayName, int priority, List<String> permissions, String parentGroup) {
        this.name = name;
        this.displayName = displayName;
        this.priority = priority;
        this.permissions = permissions;
        this.parentGroup = parentGroup;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public int getPriority() { return priority; }
    public List<String> getPermissions() { return permissions; }
    public String getParentGroup() { return parentGroup; }
}