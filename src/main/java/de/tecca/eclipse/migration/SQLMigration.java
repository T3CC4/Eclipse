package de.tecca.eclipse.migration;

import java.util.List;

/**
 * SQL Migration specifically for database operations
 */
abstract class SQLMigration extends Migration {

    public SQLMigration(String id, String version, String description) {
        super(id, version, description);
    }

    /**
     * Get SQL statements for upgrade
     */
    public abstract List<String> getUpSQL();

    /**
     * Get SQL statements for downgrade
     */
    public abstract List<String> getDownSQL();

    @Override
    public void up() throws Exception {
        // Implementation would execute SQL statements
        // This would be handled by the migration system
    }

    @Override
    public void down() throws Exception {
        // Implementation would execute SQL statements
        // This would be handled by the migration system
    }
}
