package de.tecca.eclipse.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Security configuration
 */
public class SecurityConfig {
    private final Map<String, Long> commandCooldowns;
    private int maxLoginsPerMinute = 10;
    private int maxCommandsPerSecond = 5;
    private int maxChatPerSecond = 3;
    private int spamThreshold = 3;
    private long spamWindowMs = 10000; // 10 seconds
    private int autoBlockThreshold = 5;
    private long autoBlockDurationMs = 3600000; // 1 hour

    public SecurityConfig() {
        this.commandCooldowns = new HashMap<>();
        loadDefaultCooldowns();
    }

    private void loadDefaultCooldowns() {
        // Default command cooldowns (in milliseconds)
        commandCooldowns.put("tp", 2000L);
        commandCooldowns.put("teleport", 2000L);
        commandCooldowns.put("home", 5000L);
        commandCooldowns.put("spawn", 3000L);
        commandCooldowns.put("kit", 10000L);
        commandCooldowns.put("heal", 30000L);
        commandCooldowns.put("feed", 20000L);
    }

    // Getters and setters
    public int getMaxLoginsPerMinute() { return maxLoginsPerMinute; }
    public void setMaxLoginsPerMinute(int maxLoginsPerMinute) { this.maxLoginsPerMinute = maxLoginsPerMinute; }

    public int getMaxCommandsPerSecond() { return maxCommandsPerSecond; }
    public void setMaxCommandsPerSecond(int maxCommandsPerSecond) { this.maxCommandsPerSecond = maxCommandsPerSecond; }

    public int getMaxChatPerSecond() { return maxChatPerSecond; }
    public void setMaxChatPerSecond(int maxChatPerSecond) { this.maxChatPerSecond = maxChatPerSecond; }

    public int getSpamThreshold() { return spamThreshold; }
    public void setSpamThreshold(int spamThreshold) { this.spamThreshold = spamThreshold; }

    public long getSpamWindowMs() { return spamWindowMs; }
    public void setSpamWindowMs(long spamWindowMs) { this.spamWindowMs = spamWindowMs; }

    public int getAutoBlockThreshold() { return autoBlockThreshold; }
    public void setAutoBlockThreshold(int autoBlockThreshold) { this.autoBlockThreshold = autoBlockThreshold; }

    public long getAutoBlockDurationMs() { return autoBlockDurationMs; }
    public void setAutoBlockDurationMs(long autoBlockDurationMs) { this.autoBlockDurationMs = autoBlockDurationMs; }

    public Long getCommandCooldown(String command) {
        return commandCooldowns.get(command.toLowerCase());
    }

    public void setCommandCooldown(String command, long cooldownMs) {
        commandCooldowns.put(command.toLowerCase(), cooldownMs);
    }

    public void removeCommandCooldown(String command) {
        commandCooldowns.remove(command.toLowerCase());
    }

    public Map<String, Long> getAllCommandCooldowns() {
        return new HashMap<>(commandCooldowns);
    }
}
