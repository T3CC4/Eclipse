package de.tecca.eclipse.migration;

import java.util.List;

/**
 * Current migration status
 */
public class MigrationStatus {
    private final List<String> executedMigrations;
    private final List<String> pendingMigrations;
    private final String currentVersion;

    public MigrationStatus(List<String> executedMigrations, List<String> pendingMigrations, String currentVersion) {
        this.executedMigrations = executedMigrations;
        this.pendingMigrations = pendingMigrations;
        this.currentVersion = currentVersion;
    }

    public List<String> getExecutedMigrations() {
        return executedMigrations;
    }

    public List<String> getPendingMigrations() {
        return pendingMigrations;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public boolean hasPendingMigrations() {
        return !pendingMigrations.isEmpty();
    }

    public int getTotalExecuted() {
        return executedMigrations.size();
    }

    public int getTotalPending() {
        return pendingMigrations.size();
    }

    @Override
    public String toString() {
        return String.format("MigrationStatus{version='%s', executed=%d, pending=%d}",
                currentVersion, getTotalExecuted(), getTotalPending());
    }
}