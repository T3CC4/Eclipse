package de.tecca.eclipse.api.events;

import java.util.HashMap;
import java.util.Map;

public abstract class EclipseEvent {
    private boolean cancelled = false;
    private final boolean cancellable;
    private final long timestamp = System.currentTimeMillis();
    private final Map<String, Object> data = new HashMap<>();
    private final boolean async;

    protected EclipseEvent(boolean cancellable, boolean async) {
        this.cancellable = cancellable;
        this.async = async;
    }

    protected EclipseEvent() {
        this(false, false);
    }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) {
        if (!cancellable) throw new UnsupportedOperationException("Event is not cancellable");
        this.cancelled = cancelled;
    }
    public boolean isCancellable() { return cancellable; }

    public void setData(String key, Object value) { data.put(key, value); }
    public <T> T getData(String key, Class<T> type) { return type.cast(data.get(key)); }
    public Object getData(String key) { return data.get(key); }
    public Map<String, Object> getAllData() { return new HashMap<>(data); }

    public long getTimestamp() { return timestamp; }
    public boolean isAsync() { return async; }

    public abstract String getEventName();
}