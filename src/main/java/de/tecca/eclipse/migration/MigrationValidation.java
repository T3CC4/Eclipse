package de.tecca.eclipse.migration;

import java.util.List;

/**
 * Migration validation result
 */
class MigrationValidation {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    public MigrationValidation(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors;
        this.warnings = warnings;
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
