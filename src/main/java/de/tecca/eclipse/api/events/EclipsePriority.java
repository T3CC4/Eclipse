package de.tecca.eclipse.api.events;

public enum EclipsePriority {
    LOWEST(-2),
    LOW(-1),
    NORMAL(0),
    HIGH(1),
    HIGHEST(2),
    MONITOR(3);

    private final int level;

    EclipsePriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }
}