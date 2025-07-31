package de.tecca.eclipse.event;

/**
 * Interface for Eclipse event listeners
 * @param <T> The event type this listener handles
 */
public interface EventListener<T extends EclipseEvent> {

    /**
     * Called when the event is fired
     * @param event The event that was fired
     */
    void onEvent(T event);

    /**
     * Get the event type this listener handles
     * @return Event class type
     */
    Class<T> getEventType();

    /**
     * Whether this listener should be executed asynchronously
     * @return true if async execution is preferred
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * Get the priority of this listener (lower numbers = higher priority)
     * @return Priority value
     */
    default int getPriority() {
        return 100;
    }
}