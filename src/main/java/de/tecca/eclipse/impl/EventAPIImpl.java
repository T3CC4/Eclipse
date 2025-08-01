package de.tecca.eclipse.impl;

import de.tecca.eclipse.api.EventAPI;
import de.tecca.eclipse.api.events.*;
import de.tecca.eclipse.events.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import java.util.function.Consumer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventAPIImpl implements EventAPI {

    private final Plugin plugin;
    private final EventBus eventBus;
    private final BukkitEventHandler bukkitHandler;
    private final List<EventSubscription> subscriptions = new CopyOnWriteArrayList<>();

    public EventAPIImpl(Plugin plugin) {
        this.plugin = plugin;
        this.eventBus = new EventBus();
        this.bukkitHandler = new BukkitEventHandler(plugin);
    }

    @Override
    public <T extends Event> EventSubscription hook(Class<T> eventClass, Consumer<T> handler) {
        return hook(eventClass, EventPriority.NORMAL, handler);
    }

    @Override
    public <T extends Event> EventSubscription hook(Class<T> eventClass, EventPriority priority, Consumer<T> handler) {
        return hook(eventClass, priority, false, handler);
    }

    @Override
    public <T extends Event> EventSubscription hook(Class<T> eventClass, EventPriority priority, boolean ignoreCancelled, Consumer<T> handler) {
        EventSubscription subscription = bukkitHandler.hook(eventClass, priority, ignoreCancelled, handler);
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public <T> void publish(String eventName, T data) {
        eventBus.publish(eventName, data);
    }

    @Override
    public <T> void publish(Class<T> eventClass, T data) {
        eventBus.publish(eventClass.getSimpleName(), data);
    }

    @Override
    public void publish(EclipseEvent event) {
        eventBus.publish(event);
    }

    @Override
    public <T> EventSubscription subscribe(String eventName, Class<T> dataType, Consumer<T> handler) {
        return subscribe(eventName, dataType, EclipsePriority.NORMAL, handler);
    }

    @Override
    public <T> EventSubscription subscribe(Class<T> eventClass, Consumer<T> handler) {
        return subscribe(eventClass.getSimpleName(), eventClass, handler);
    }

    @Override
    public <T> EventSubscription subscribe(String eventName, Class<T> dataType, EclipsePriority priority, Consumer<T> handler) {
        EventSubscription subscription = eventBus.subscribe(eventName, dataType, priority, handler);
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public <T extends EclipseEvent> EventBuilder<T> createEvent(Class<T> eventClass) {
        return eventBus.createEvent(eventClass);
    }

    @Override
    public EventBuilder<GenericEclipseEvent> createEvent(String eventName) {
        return eventBus.createEvent(eventName);
    }

    @Override
    public void unsubscribe(EventSubscription subscription) {
        subscription.cancel();
        subscriptions.remove(subscription);
    }

    @Override
    public void unsubscribeAll(Object owner) {
        subscriptions.removeIf(subscription -> {
            subscription.cancel();
            return true;
        });
    }

    @Override
    public List<String> getSubscribedEvents() {
        return eventBus.getSubscribedEvents();
    }

    @Override
    public int getActiveSubscriptions() {
        return subscriptions.size();
    }

    @Override
    public void shutdown() {
        subscriptions.forEach(EventSubscription::cancel);
        subscriptions.clear();
        bukkitHandler.shutdown();
        eventBus.shutdown();
    }
}