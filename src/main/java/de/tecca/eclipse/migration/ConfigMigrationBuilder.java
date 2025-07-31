package de.tecca.eclipse.migration;

import java.io.File;

/**
 * Configuration migration builder
 */
class ConfigMigrationBuilder {
    private String id;
    private String fromVersion;
    private String toVersion;
    private String description;
    private ConfigMigrationAction action;

    public ConfigMigrationBuilder id(String id) {
        this.id = id;
        return this;
    }

    public ConfigMigrationBuilder fromVersion(String fromVersion) {
        this.fromVersion = fromVersion;
        return this;
    }

    public ConfigMigrationBuilder toVersion(String toVersion) {
        this.toVersion = toVersion;
        return this;
    }

    public ConfigMigrationBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ConfigMigrationBuilder migrate(ConfigMigrationAction action) {
        this.action = action;
        return this;
    }

    public ConfigMigration build() {
        if (id == null || fromVersion == null || toVersion == null || description == null) {
            throw new IllegalStateException("All fields are required for config migration");
        }

        return new ConfigMigration(id, fromVersion, toVersion, description) {
            @Override
            public void migrate(File configFile) throws Exception {
                if (action != null) {
                    action.migrate(configFile);
                }
            }
        };
    }

    @FunctionalInterface
    public interface ConfigMigrationAction {
        void migrate(File configFile) throws Exception;
    }
}
