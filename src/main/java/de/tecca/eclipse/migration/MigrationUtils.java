package de.tecca.eclipse.migration;

/**
 * Utility class for common migration operations
 */
class MigrationUtils {

    /**
     * Safely add column to table if it doesn't exist
     */
    public static String addColumnIfNotExists(String tableName, String columnName, String columnType) {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s %s",
                tableName, columnName, columnType);
    }

    /**
     * Safely drop column from table if it exists
     */
    public static String dropColumnIfExists(String tableName, String columnName) {
        return String.format("ALTER TABLE %s DROP COLUMN IF EXISTS %s", tableName, columnName);
    }

    /**
     * Create index if not exists
     */
    public static String createIndexIfNotExists(String indexName, String tableName, String... columns) {
        return String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s)",
                indexName, tableName, String.join(", ", columns));
    }

    /**
     * Drop index if exists
     */
    public static String dropIndexIfExists(String indexName) {
        return String.format("DROP INDEX IF EXISTS %s", indexName);
    }

    /**
     * Rename table
     */
    public static String renameTable(String oldName, String newName) {
        return String.format("ALTER TABLE %s RENAME TO %s", oldName, newName);
    }

    /**
     * Create backup table
     */
    public static String createBackupTable(String originalTable, String backupTable) {
        return String.format("CREATE TABLE %s AS SELECT * FROM %s", backupTable, originalTable);
    }

    /**
     * Check if table exists (SQLite)
     */
    public static String checkTableExistsSQLite(String tableName) {
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", tableName);
    }

    /**
     * Check if table exists (MySQL)
     */
    public static String checkTableExistsMySQL(String tableName, String databaseName) {
        return String.format("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s'",
                databaseName, tableName);
    }

    /**
     * Get table column info (SQLite)
     */
    public static String getTableInfoSQLite(String tableName) {
        return String.format("PRAGMA table_info(%s)", tableName);
    }

    /**
     * Get table column info (MySQL)
     */
    public static String getTableInfoMySQL(String tableName, String databaseName) {
        return String.format("SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s'",
                databaseName, tableName);
    }
}
