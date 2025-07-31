package de.tecca.eclipse.migration;

/**
 * Data migration for moving/transforming data
 */
abstract class DataMigration extends Migration {

    public DataMigration(String id, String version, String description) {
        super(id, version, description);
    }

    /**
     * Transform data during upgrade
     */
    public abstract void migrateData() throws Exception;

    /**
     * Reverse data transformation during downgrade
     */
    public abstract void rollbackData() throws Exception;

    @Override
    public void up() throws Exception {
        migrateData();
    }

    @Override
    public void down() throws Exception {
        rollbackData();
    }
}

