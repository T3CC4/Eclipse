package de.tecca.eclipse.api.events;

public interface EventBuilder<T extends EclipseEvent> {
    EventBuilder<T> data(String key, Object value);
    EventBuilder<T> cancellable(boolean cancellable);
    EventBuilder<T> async(boolean async);

    T fire();
    EventResult<T> fireAndWait();
}