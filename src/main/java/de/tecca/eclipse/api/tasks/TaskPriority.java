package de.tecca.eclipse.api.tasks;

public enum TaskPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4),
    CRITICAL(5);

    private final int level;

    TaskPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }
}