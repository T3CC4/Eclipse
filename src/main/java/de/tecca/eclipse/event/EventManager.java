package de.tecca.eclipse.event;

import de.tecca.eclipse.Eclipse;
import org.bukkit.Bukkit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages Eclipse events and listeners
 */
public class EventManager {

    private final Eclipse plugin;
    private final Map<Class<? extends EclipseEvent>, List<EventListener>> listeners;

    public EventManager(Eclipse plugin) {
        this.plugin = plugin;
        this.listeners = new ConcurrentHashMap<>();
    }

    /**
     * Register an event listener
     * @param listener The listener to register
     */
    public void registerListener(EventListener listener) {
        Class<? extends EclipseEvent> eventType = listener.getEventType();
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Unregister an event listener
     * @param listener The listener to unregister
     */
    public void unregisterListener(EventListener listener) {
        Class<? extends EclipseEvent> eventType = listener.getEventType();
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }

    /**
     * Fire an Eclipse event
     * @param event The event to fire
     */
    public void fireEvent(EclipseEvent event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                try {
                    if (listener.isAsync()) {
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> listener.onEvent(event));
                    } else {
                        listener.onEvent(event);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error executing event listener: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Fire an Eclipse event asynchronously
     * @param event The event to fire
     */
    public void fireEventAsync(EclipseEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fireEvent(event));
    }

    /**
     * Get the number of registered listeners for an event type
     * @param eventType The event type
     * @return Number of listeners
     */
    public int getListenerCount(Class<? extends EclipseEvent> eventType) {
        List<EventListener> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }

    /**
     * Clear all listeners for a specific event type
     * @param eventType The event type to clear
     */
    public void clearListeners(Class<? extends EclipseEvent> eventType) {
        listeners.remove(eventType);
    }

    /**
     * Clear all listeners
     */
    public void clearAllListeners() {
        listeners.clear();
    }

    /**
     * Shutdown the event manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Event Manager...");

        // Cancel all pending async event executions
        for (List<EventListener> eventListeners : listeners.values()) {
            for (EventListener listener : eventListeners) {
                try {
                    // If listener has cleanup method, call it
                    if (listener instanceof AutoCloseable) {
                        ((AutoCloseable) listener).close();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error cleaning up listener: " + e.getMessage());
                }
            }
        }

        // Clear all listeners
        clearAllListeners();

        // Cancel any running Bukkit tasks related to async event firing
        plugin.getServer().getScheduler().cancelTasks(plugin);

        plugin.getLogger().info("Event Manager shutdown complete - " + listeners.size() + " listener types cleared");
    }
}