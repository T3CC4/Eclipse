package de.tecca.eclipse.security;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting implementation
 */
class RateLimit {
    private final int maxAttempts;
    private final long windowMs;
    private final Queue<Long> attempts;

    public RateLimit(int maxAttempts, long windowMs) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
        this.attempts = new LinkedList<>();
    }

    public synchronized boolean tryAction() {
        long now = System.currentTimeMillis();

        // Remove expired attempts
        attempts.removeIf(time -> now - time > windowMs);

        // Check if we can add another attempt
        if (attempts.size() >= maxAttempts) {
            return false;
        }

        attempts.offer(now);
        return true;
    }

    public boolean isExpired(long currentTime) {
        return attempts.isEmpty() || (currentTime - attempts.peek()) > windowMs * 2;
    }

    public int getRemainingAttempts() {
        return Math.max(0, maxAttempts - attempts.size());
    }
}