package de.tecca.eclipse.migration;

import de.tecca.eclipse.Eclipse;
import de.tecca.eclipse.database.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Professional database and configuration migration system
 */
public class MigrationSystem {

    private final Eclipse plugin;
    private final DatabaseManager database;
    private final Map<String, Migration> migrations;
    private final Map<String, ConfigMigration> configMigrations;
    private String currentVersion;

    public MigrationSystem(Eclipse plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.migrations = new ConcurrentHashMap<>();
        this.configMigrations = new ConcurrentHashMap<>();
        this.currentVersion = plugin.getDescription().getVersion();

        setupMigrationTracking();
        registerDefaultMigrations();
    }

    /**
     * Setup migration tracking table/storage
     */
    private void setupMigrationTracking() {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            // JSON doesn't need table creation
            return;
        }

        database.createTableIfNotExists("eclipse_migrations",
                "(id VARCHAR(100) PRIMARY KEY, " +
                        "version VARCHAR(20), " +
                        "description TEXT, " +
                        "executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "execution_time_ms BIGINT, " +
                        "checksum VARCHAR(64))");
    }

    /**
     * Register default migrations
     */
    private void registerDefaultMigrations() {
        // Example migration: Initial schema setup
        registerMigration(new Migration("001_initial_setup", "1.0", "Initial database setup") {
            @Override
            public void up() throws Exception {
                if (database.getDatabaseType() != DatabaseManager.DatabaseType.JSON) {
                    database.createTableIfNotExists("eclipse_users",
                            "(uuid VARCHAR(36) PRIMARY KEY, " +
                                    "username VARCHAR(16), " +
                                    "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                    "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                    "play_time BIGINT DEFAULT 0)");
                }
            }

            @Override
            public void down() throws Exception {
                if (database.getDatabaseType() != DatabaseManager.DatabaseType.JSON) {
                    database.executeUpdate("DROP TABLE IF EXISTS eclipse_users");
                }
            }
        });

        // Example: Add new columns
        registerMigration(new Migration("002_add_user_stats", "1.1", "Add user statistics columns") {
            @Override
            public void up() throws Exception {
                if (database.getDatabaseType() != DatabaseManager.DatabaseType.JSON) {
                    database.executeUpdate("ALTER TABLE eclipse_users ADD COLUMN IF NOT EXISTS blocks_broken BIGINT DEFAULT 0");
                    database.executeUpdate("ALTER TABLE eclipse_users ADD COLUMN IF NOT EXISTS blocks_placed BIGINT DEFAULT 0");
                    database.executeUpdate("ALTER TABLE eclipse_users ADD COLUMN IF NOT EXISTS deaths INT DEFAULT 0");
                }
            }

            @Override
            public void down() throws Exception {
                if (database.getDatabaseType() != DatabaseManager.DatabaseType.JSON) {
                    database.executeUpdate("ALTER TABLE eclipse_users DROP COLUMN IF EXISTS blocks_broken");
                    database.executeUpdate("ALTER TABLE eclipse_users DROP COLUMN IF EXISTS blocks_placed");
                    database.executeUpdate("ALTER TABLE eclipse_users DROP COLUMN IF EXISTS deaths");
                }
            }
        });

        // Config migration example
        registerConfigMigration(new ConfigMigration("config_v1_to_v2", "1.0", "2.0", "Update config format") {
            @Override
            public void migrate(File configFile) throws Exception {
                // Example: Rename configuration keys
                var config = plugin.getConfigManager().loadConfig("config", false);
                if (config.contains("old-setting")) {
                    Object value = config.get("old-setting");
                    config.set("new-setting", value);
                    config.set("old-setting", null);
                    plugin.getConfigManager().saveConfig("config");
                }
            }
        });
    }

    /**
     * Register a database migration
     */
    public void registerMigration(Migration migration) {
        migrations.put(migration.getId(), migration);
        plugin.getLogger().info("Registered migration: " + migration.getId());
    }

    /**
     * Register a configuration migration
     */
    public void registerConfigMigration(ConfigMigration migration) {
        configMigrations.put(migration.getId(), migration);
        plugin.getLogger().info("Registered config migration: " + migration.getId());
    }

    /**
     * Run all pending migrations
     */
    public CompletableFuture<MigrationResult> runMigrations() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Starting migration process...");

                // Backup before migrations
                createBackup().join();

                // Run database migrations
                List<String> executedMigrations = new ArrayList<>();
                List<String> failedMigrations = new ArrayList<>();

                for (Migration migration : getSortedMigrations()) {
                    if (!isMigrationExecuted(migration.getId())) {
                        try {
                            executeMigration(migration);
                            executedMigrations.add(migration.getId());
                            plugin.getLogger().info("✓ Executed migration: " + migration.getId());
                        } catch (Exception e) {
                            failedMigrations.add(migration.getId());
                            plugin.getLogger().severe("✗ Failed migration: " + migration.getId() + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                // Run config migrations
                runConfigMigrations();

                plugin.getLogger().info("Migration process completed. Executed: " + executedMigrations.size() +
                        ", Failed: " + failedMigrations.size());

                return new MigrationResult(executedMigrations, failedMigrations);

            } catch (Exception e) {
                plugin.getLogger().severe("Migration process failed: " + e.getMessage());
                e.printStackTrace();
                return new MigrationResult(Collections.emptyList(),
                        migrations.keySet().stream().toList());
            }
        });
    }

    /**
     * Execute a single migration
     */
    private void executeMigration(Migration migration) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            migration.up();
            long executionTime = System.currentTimeMillis() - startTime;

            // Record successful migration
            recordMigration(migration, executionTime);

        } catch (Exception e) {
            // If migration fails, try to rollback
            try {
                migration.down();
                plugin.getLogger().warning("Rolled back failed migration: " + migration.getId());
            } catch (Exception rollbackError) {
                plugin.getLogger().severe("Failed to rollback migration: " + migration.getId());
                rollbackError.printStackTrace();
            }
            throw e;
        }
    }

    /**
     * Record executed migration
     */
    private void recordMigration(Migration migration, long executionTime) {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            List<Map<String, Object>> executedMigrations = database.getJSONData("executed_migrations", List.class, new ArrayList<>());

            Map<String, Object> migrationRecord = new HashMap<>();
            migrationRecord.put("id", migration.getId());
            migrationRecord.put("version", migration.getVersion());
            migrationRecord.put("description", migration.getDescription());
            migrationRecord.put("executedAt", System.currentTimeMillis());
            migrationRecord.put("executionTimeMs", executionTime);
            migrationRecord.put("checksum", migration.getChecksum());

            executedMigrations.add(migrationRecord);
            database.setJSONData("executed_migrations", executedMigrations);
        } else {
            database.executeUpdate(
                    "INSERT INTO eclipse_migrations (id, version, description, execution_time_ms, checksum) VALUES (?, ?, ?, ?, ?)",
                    migration.getId(), migration.getVersion(), migration.getDescription(),
                    executionTime, migration.getChecksum()
            );
        }
    }

    /**
     * Check if migration has been executed
     */
    private boolean isMigrationExecuted(String migrationId) {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            List<Map<String, Object>> executedMigrations = database.getJSONData("executed_migrations", List.class, new ArrayList<>());
            return executedMigrations.stream()
                    .anyMatch(m -> migrationId.equals(m.get("id")));
        } else {
            return database.executeQueryWithResult(
                    "SELECT COUNT(*) FROM eclipse_migrations WHERE id = ?",
                    rs -> {
                        try {
                            return rs.next() && rs.getInt(1) > 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    migrationId
            );
        }
    }

    /**
     * Get migrations sorted by ID (which should include version info)
     */
    private List<Migration> getSortedMigrations() {
        return migrations.values().stream()
                .sorted(Comparator.comparing(Migration::getId))
                .toList();
    }

    /**
     * Run configuration migrations
     */
    private void runConfigMigrations() {
        String currentConfigVersion = getCurrentConfigVersion();

        for (ConfigMigration migration : configMigrations.values()) {
            if (shouldRunConfigMigration(migration, currentConfigVersion)) {
                try {
                    File configFile = new File(plugin.getDataFolder(), "config.yml");
                    if (configFile.exists()) {
                        migration.migrate(configFile);
                        plugin.getLogger().info("✓ Executed config migration: " + migration.getId());
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("✗ Failed config migration: " + migration.getId() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Update config version
        updateConfigVersion(currentVersion);
    }

    /**
     * Check if config migration should run
     */
    private boolean shouldRunConfigMigration(ConfigMigration migration, String currentConfigVersion) {
        return compareVersions(currentConfigVersion, migration.getFromVersion()) >= 0 &&
                compareVersions(currentConfigVersion, migration.getToVersion()) < 0;
    }

    /**
     * Get current configuration version
     */
    private String getCurrentConfigVersion() {
        var config = plugin.getConfigManager().getConfig("config");
        return config != null ? config.getString("config-version", "1.0") : "1.0";
    }

    /**
     * Update configuration version
     */
    private void updateConfigVersion(String version) {
        var config = plugin.getConfigManager().getConfig("config");
        if (config != null) {
            config.set("config-version", version);
            plugin.getConfigManager().saveConfig("config");
        }
    }

    /**
     * Create backup before migrations
     */
    public CompletableFuture<String> createBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String backupName = "eclipse_backup_" + timestamp;

                File backupDir = new File(plugin.getDataFolder(), "backups/" + backupName);
                backupDir.mkdirs();

                // Backup configuration files
                backupConfigs(backupDir);

                // Backup database
                if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
                    backupJSONData(backupDir);
                } else {
                    backupSQLDatabase(backupDir, backupName);
                }

                plugin.getLogger().info("Created backup: " + backupName);
                return backupName;

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Backup configuration files
     */
    private void backupConfigs(File backupDir) throws IOException {
        File configsBackup = new File(backupDir, "configs");
        configsBackup.mkdirs();

        File dataFolder = plugin.getDataFolder();
        for (File file : Objects.requireNonNull(dataFolder.listFiles())) {
            if (file.isFile() && (file.getName().endsWith(".yml") || file.getName().endsWith(".json"))) {
                Files.copy(file.toPath(),
                        new File(configsBackup, file.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Backup JSON data
     */
    private void backupJSONData(File backupDir) throws IOException {
        File jsonBackup = new File(backupDir, "data");
        jsonBackup.mkdirs();

        // Copy all JSON data files
        File dataFolder = plugin.getDataFolder();
        for (File file : Objects.requireNonNull(dataFolder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                Files.copy(file.toPath(),
                        new File(jsonBackup, file.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Backup SQL database
     */
    private void backupSQLDatabase(File backupDir, String backupName) {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.SQLITE) {
            // For SQLite, just copy the database file
            try {
                // This would need to be adapted based on the actual SQLite file location
                plugin.getLogger().info("SQLite backup would be implemented here");
            } catch (Exception e) {
                plugin.getLogger().warning("SQLite backup failed: " + e.getMessage());
            }
        } else if (database.getDatabaseType() == DatabaseManager.DatabaseType.MYSQL) {
            // For MySQL, create a SQL dump
            plugin.getLogger().info("MySQL backup would require mysqldump or similar tool");
        }
    }

    /**
     * Rollback to a specific migration
     */
    public CompletableFuture<Boolean> rollbackTo(String migrationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Starting rollback to migration: " + migrationId);

                // Create backup before rollback
                createBackup().join();

                // Get migrations to rollback (in reverse order)
                List<Migration> migrationsToRollback = getSortedMigrations().stream()
                        .filter(m -> compareVersions(m.getVersion(), migrations.get(migrationId).getVersion()) > 0)
                        .sorted((a, b) -> compareVersions(b.getVersion(), a.getVersion()))
                        .toList();

                for (Migration migration : migrationsToRollback) {
                    if (isMigrationExecuted(migration.getId())) {
                        try {
                            migration.down();
                            removeMigrationRecord(migration.getId());
                            plugin.getLogger().info("✓ Rolled back migration: " + migration.getId());
                        } catch (Exception e) {
                            plugin.getLogger().severe("✗ Failed to rollback migration: " + migration.getId());
                            e.printStackTrace();
                            return false;
                        }
                    }
                }

                plugin.getLogger().info("Rollback completed successfully");
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("Rollback failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Remove migration record
     */
    private void removeMigrationRecord(String migrationId) {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            List<Map<String, Object>> executedMigrations = database.getJSONData("executed_migrations", List.class, new ArrayList<>());
            executedMigrations.removeIf(m -> migrationId.equals(m.get("id")));
            database.setJSONData("executed_migrations", executedMigrations);
        } else {
            database.executeUpdate("DELETE FROM eclipse_migrations WHERE id = ?", migrationId);
        }
    }

    /**
     * Get migration status
     */
    public MigrationStatus getStatus() {
        List<String> executed = new ArrayList<>();
        List<String> pending = new ArrayList<>();

        for (Migration migration : migrations.values()) {
            if (isMigrationExecuted(migration.getId())) {
                executed.add(migration.getId());
            } else {
                pending.add(migration.getId());
            }
        }

        return new MigrationStatus(executed, pending, currentVersion);
    }

    /**
     * Compare version strings
     */
    private int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }

        return 0;
    }

    /**
     * Get all available backups
     */
    public List<String> getAvailableBackups() {
        File backupsDir = new File(plugin.getDataFolder(), "backups");
        if (!backupsDir.exists()) {
            return Collections.emptyList();
        }

        return Arrays.stream(Objects.requireNonNull(backupsDir.listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .sorted(Collections.reverseOrder())
                .toList();
    }

    /**
     * Restore from backup
     */
    public CompletableFuture<Boolean> restoreFromBackup(String backupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File backupDir = new File(plugin.getDataFolder(), "backups/" + backupName);
                if (!backupDir.exists()) {
                    plugin.getLogger().severe("Backup not found: " + backupName);
                    return false;
                }

                plugin.getLogger().info("Starting restore from backup: " + backupName);

                // Restore configs
                File configsBackup = new File(backupDir, "configs");
                if (configsBackup.exists()) {
                    for (File file : Objects.requireNonNull(configsBackup.listFiles())) {
                        Files.copy(file.toPath(),
                                new File(plugin.getDataFolder(), file.getName()).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                // Restore data
                File dataBackup = new File(backupDir, "data");
                if (dataBackup.exists()) {
                    for (File file : Objects.requireNonNull(dataBackup.listFiles())) {
                        Files.copy(file.toPath(),
                                new File(plugin.getDataFolder(), file.getName()).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                plugin.getLogger().info("Restore completed successfully");
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("Restore failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Clean up old backups (keep last N backups)
     */
    public void cleanupOldBackups(int keepCount) {
        List<String> backups = getAvailableBackups();
        if (backups.size() <= keepCount) {
            return;
        }

        List<String> toDelete = backups.subList(keepCount, backups.size());

        for (String backup : toDelete) {
            try {
                File backupDir = new File(plugin.getDataFolder(), "backups/" + backup);
                deleteDirectory(backupDir);
                plugin.getLogger().info("Deleted old backup: " + backup);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete backup: " + backup);
            }
        }
    }

    /**
     * Delete directory recursively
     */
    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                deleteDirectory(file);
            }
        }
        if (!dir.delete()) {
            throw new IOException("Failed to delete: " + dir.getAbsolutePath());
        }
    }

    /**
     * Shutdown migration system
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Migration System...");

        // No active migrations should be running, but log status
        MigrationStatus status = getStatus();

        // Clear in-memory migration registry
        int migrationCount = migrations.size();
        int configMigrationCount = configMigrations.size();

        migrations.clear();
        configMigrations.clear();

        // Clean up old backups (keep last 5)
        try {
            cleanupOldBackups(5);
            plugin.getLogger().info("Backup cleanup completed");
        } catch (Exception e) {
            plugin.getLogger().warning("Error during backup cleanup: " + e.getMessage());
        }

        plugin.getLogger().info("Migration System shutdown complete - " +
                "Cleared " + migrationCount + " migrations, " +
                configMigrationCount + " config migrations. " +
                "Status: " + status.getTotalExecuted() + " executed, " +
                status.getTotalPending() + " pending");
    }
}