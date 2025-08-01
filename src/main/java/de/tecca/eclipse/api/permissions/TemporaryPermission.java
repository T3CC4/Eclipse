package de.tecca.eclipse.api.permissions;

import java.util.UUID;

public class TemporaryPermission {
    private final UUID playerUuid;
    private final String permission;
    private final long expiresAt;
    private final UUID grantedBy;

    public TemporaryPermission(UUID playerUuid, String permission, long expiresAt, UUID grantedBy) {
        this.playerUuid = playerUuid;
        this.permission = permission;
        this.expiresAt = expiresAt;
        this.grantedBy = grantedBy;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPermission() { return permission; }
    public long getExpiresAt() { return expiresAt; }
    public UUID getGrantedBy() { return grantedBy; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public long getTimeRemaining() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
}