package de.tecca.eclipse.migration;

/**
 * Migration exception for handling migration-specific errors
 */
class MigrationException extends Exception {
    private final String migrationId;

    public MigrationException(String migrationId, String message) {
        super(message);
        this.migrationId = migrationId;
    }

    public MigrationException(String migrationId, String message, Throwable cause) {
        super(message, cause);
        this.migrationId = migrationId;
    }

    public String getMigrationId() {
        return migrationId;
    }
}