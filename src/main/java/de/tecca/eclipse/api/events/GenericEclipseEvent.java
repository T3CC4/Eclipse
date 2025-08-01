package de.tecca.eclipse.api.events;

public class GenericEclipseEvent extends EclipseEvent {
    private final String eventName;

    public GenericEclipseEvent(String eventName, boolean cancellable, boolean async) {
        super(cancellable, async);
        this.eventName = eventName;
    }

    @Override
    public String getEventName() {
        return eventName;
    }
}