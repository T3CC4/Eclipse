package de.tecca.eclipse.migration;

/**
 * Migration builder for creating migrations programmatically
 */
class MigrationBuilder {
    private String id;
    private String version;
    private String description;
    private MigrationAction upAction;
    private MigrationAction downAction;

    public MigrationBuilder id(String id) {
        this.id = id;
        return this;
    }

    public MigrationBuilder version(String version) {
        this.version = version;
        return this;
    }

    public MigrationBuilder description(String description) {
        this.description = description;
        return this;
    }

    public MigrationBuilder up(MigrationAction upAction) {
        this.upAction = upAction;
        return this;
    }

    public MigrationBuilder down(MigrationAction downAction) {
        this.downAction = downAction;
        return this;
    }

    public Migration build() {
        if (id == null || version == null || description == null) {
            throw new IllegalStateException("ID, version, and description are required");
        }

        return new Migration(id, version, description) {
            @Override
            public void up() throws Exception {
                if (upAction != null) {
                    upAction.execute();
                }
            }

            @Override
            public void down() throws Exception {
                if (downAction != null) {
                    downAction.execute();
                }
            }
        };
    }

    @FunctionalInterface
    public interface MigrationAction {
        void execute() throws Exception;
    }
}