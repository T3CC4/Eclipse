package de.tecca.eclipse.api.events;

public class EventResult<T extends EclipseEvent> {
    private final T event;
    private final boolean success;
    private final Exception exception;

    public EventResult(T event, boolean success, Exception exception) {
        this.event = event;
        this.success = success;
        this.exception = exception;
    }

    public T getEvent() { return event; }
    public boolean isSuccess() { return success; }
    public Exception getException() { return exception; }
}