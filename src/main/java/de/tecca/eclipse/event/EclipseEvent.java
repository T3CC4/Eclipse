package de.tecca.eclipse.event;

/**
 * Base class for all Eclipse events
 */
public abstract class EclipseEvent {

    private final long timestamp;
    private boolean cancelled = false;

    public EclipseEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get the timestamp when this event was created
     * @return Timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Check if this event is cancelled
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Set the cancelled state of this event
     * @param cancelled true to cancel the event
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Get the event name
     * @return Event name
     */
    public String getEventName() {
        return this.getClass().getSimpleName();
    }
}