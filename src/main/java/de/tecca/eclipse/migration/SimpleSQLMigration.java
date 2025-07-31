package de.tecca.eclipse.migration;

import java.util.List;

/**
 * Simple SQL migration implementation
 */
class SimpleSQLMigration extends SQLMigration {
    private final List<String> upStatements;
    private final List<String> downStatements;

    public SimpleSQLMigration(String id, String version, String description,
                              List<String> upStatements, List<String> downStatements) {
        super(id, version, description);
        this.upStatements = upStatements;
        this.downStatements = downStatements;
    }

    @Override
    public List<String> getUpSQL() {
        return upStatements;
    }

    @Override
    public List<String> getDownSQL() {
        return downStatements;
    }
}