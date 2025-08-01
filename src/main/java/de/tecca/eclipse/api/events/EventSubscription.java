package de.tecca.eclipse.api.events;

public interface EventSubscription {
    String getEventName();
    EclipsePriority getPriority();
    boolean isActive();
    void cancel();
}