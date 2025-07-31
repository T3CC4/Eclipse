package de.tecca.eclipse.migration;

import java.io.File;
import java.security.MessageDigest;
import java.util.List;

/**
 * Abstract base class for database migrations
 */
public abstract class Migration {
    private final String id;
    private final String version;
    private final String description;

    public Migration(String id, String version, String description) {
        this.id = id;
        this.version = version;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Execute the migration (upgrade)
     */
    public abstract void up() throws Exception;

    /**
     * Rollback the migration (downgrade)
     */
    public abstract void down() throws Exception;

    /**
     * Get checksum for migration verification
     */
    public String getChecksum() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String content = id + version + description + getClass().getName();
            byte[] hash = md.digest(content.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public String toString() {
        return String.format("Migration{id='%s', version='%s', description='%s'}",
                id, version, description);
    }
}