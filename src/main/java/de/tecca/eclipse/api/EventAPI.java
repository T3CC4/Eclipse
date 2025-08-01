package de.tecca.eclipse.api;

import de.tecca.eclipse.api.events.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import java.util.function.Consumer;
import java.util.List;

public interface EventAPI {

    <T extends Event> EventSubscription hook(Class<T> eventClass, Consumer<T> handler);
    <T extends Event> EventSubscription hook(Class<T> eventClass, EventPriority priority, Consumer<T> handler);
    <T extends Event> EventSubscription hook(Class<T> eventClass, EventPriority priority, boolean ignoreCancelled, Consumer<T> handler);

    <T> void publish(String eventName, T data);
    <T> void publish(Class<T> eventClass, T data);
    void publish(EclipseEvent event);

    <T> EventSubscription subscribe(String eventName, Class<T> dataType, Consumer<T> handler);
    <T> EventSubscription subscribe(Class<T> eventClass, Consumer<T> handler);
    <T> EventSubscription subscribe(String eventName, Class<T> dataType, EclipsePriority priority, Consumer<T> handler);

    <T extends EclipseEvent> EventBuilder<T> createEvent(Class<T> eventClass);
    EventBuilder<GenericEclipseEvent> createEvent(String eventName);

    void unsubscribe(EventSubscription subscription);
    void unsubscribeAll(Object owner);

    List<String> getSubscribedEvents();
    int getActiveSubscriptions();

    void shutdown();
}