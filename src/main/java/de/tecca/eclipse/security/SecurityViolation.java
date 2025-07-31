package de.tecca.eclipse.security;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security violation record
 */
class SecurityViolation {
    private final InetAddress ip;
    private final UUID uuid;
    private final SecurityManager.ViolationType type;
    private final String details;
    private final long timestamp;
    private final AtomicInteger count;

    public SecurityViolation(InetAddress ip, UUID uuid, SecurityManager.ViolationType type, String details) {
        this.ip = ip;
        this.uuid = uuid;
        this.type = type;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
        this.count = new AtomicInteger(1);
    }

    public InetAddress getIP() { return ip; }
    public UUID getUUID() { return uuid; }
    public SecurityManager.ViolationType getType() { return type; }
    public String getDetails() { return details; }
    public long getTimestamp() { return timestamp; }
    public int getCount() { return count.get(); }

    public void incrementCount() {
        count.incrementAndGet();
    }
}