package de.tecca.eclipse.migration;

import java.util.List;

/**
 * Result of migration execution
 */
public class MigrationResult {
    private final List<String> executedMigrations;
    private final List<String> failedMigrations;
    private final boolean success;

    public MigrationResult(List<String> executedMigrations, List<String> failedMigrations) {
        this.executedMigrations = executedMigrations;
        this.failedMigrations = failedMigrations;
        this.success = failedMigrations.isEmpty();
    }

    public List<String> getExecutedMigrations() {
        return executedMigrations;
    }

    public List<String> getFailedMigrations() {
        return failedMigrations;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getTotalExecuted() {
        return executedMigrations.size();
    }

    public int getTotalFailed() {
        return failedMigrations.size();
    }

    @Override
    public String toString() {
        return String.format("MigrationResult{executed=%d, failed=%d, success=%s}",
                getTotalExecuted(), getTotalFailed(), success);
    }
}