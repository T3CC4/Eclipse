package de.tecca.eclipse.migration;

import java.io.File;

/**
 * Abstract base class for configuration migrations
 */
public abstract class ConfigMigration {
    private final String id;
    private final String fromVersion;
    private final String toVersion;
    private final String description;

    public ConfigMigration(String id, String fromVersion, String toVersion, String description) {
        this.id = id;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public String getToVersion() {
        return toVersion;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Perform the configuration migration
     */
    public abstract void migrate(File configFile) throws Exception;

    @Override
    public String toString() {
        return String.format("ConfigMigration{id='%s', from='%s', to='%s', description='%s'}",
                id, fromVersion, toVersion, description);
    }
}
