package de.tecca.eclipse.security;

import de.tecca.eclipse.Eclipse;
import de.tecca.eclipse.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Professional security management system with rate limiting, IP blocking, and anti-spam
 */
public class SecurityManager implements Listener {

    private final Eclipse plugin;
    private final DatabaseManager database;

    // Rate limiting
    private final Map<UUID, Map<String, RateLimit>> playerRateLimits;
    private final Map<InetAddress, Map<String, RateLimit>> ipRateLimits;

    // Command cooldowns
    private final Map<UUID, Map<String, Long>> commandCooldowns;

    // IP blocking
    private final Set<InetAddress> blockedIPs;
    private final Map<InetAddress, SecurityViolation> ipViolations;

    // Anti-spam
    private final Map<UUID, AntiSpamData> antiSpamData;

    // Security settings
    private final SecurityConfig config;

    public SecurityManager(Eclipse plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.playerRateLimits = new ConcurrentHashMap<>();
        this.ipRateLimits = new ConcurrentHashMap<>();
        this.commandCooldowns = new ConcurrentHashMap<>();
        this.blockedIPs = ConcurrentHashMap.newKeySet();
        this.ipViolations = new ConcurrentHashMap<>();
        this.antiSpamData = new ConcurrentHashMap<>();
        this.config = new SecurityConfig();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupDatabase();
        loadBlockedIPs();
        startCleanupTask();
    }

    /**
     * Setup database tables
     */
    private void setupDatabase() {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            return;
        }

        database.createTableIfNotExists("eclipse_security_blocks",
                "(ip VARCHAR(15) PRIMARY KEY, " +
                        "reason TEXT, " +
                        "blocked_by VARCHAR(36), " +
                        "blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "expires_at TIMESTAMP NULL)");

        database.createTableIfNotExists("eclipse_security_violations",
                "(id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "ip VARCHAR(15), " +
                        "uuid VARCHAR(36), " +
                        "violation_type VARCHAR(50), " +
                        "details TEXT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    /**
     * Load blocked IPs from database
     */
    private void loadBlockedIPs() {
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            Set<String> blocked = database.getJSONData("blocked_ips", Set.class, new HashSet<>());
            blocked.forEach(ip -> {
                try {
                    blockedIPs.add(InetAddress.getByName(ip));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid blocked IP: " + ip);
                }
            });
        } else {
            database.executeQuery(
                    "SELECT ip FROM eclipse_security_blocks WHERE expires_at IS NULL OR expires_at > NOW()",
                    rs -> {
                        try {
                            while (rs.next()) {
                                blockedIPs.add(InetAddress.getByName(rs.getString("ip")));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error loading blocked IPs: " + e.getMessage());
                        }
                    }
            );
        }

        plugin.getLogger().info("Loaded " + blockedIPs.size() + " blocked IPs");
    }

    /**
     * Start cleanup task for expired data
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();

            // Clean rate limits
            playerRateLimits.values().forEach(limits ->
                    limits.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime)));
            ipRateLimits.values().forEach(limits ->
                    limits.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime)));

            // Clean command cooldowns
            commandCooldowns.values().forEach(cooldowns ->
                    cooldowns.entrySet().removeIf(entry -> entry.getValue() < currentTime));

            // Clean anti-spam data
            antiSpamData.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));

        }, 20L * 60, 20L * 60); // Run every minute
    }

    // ===============================
    // RATE LIMITING
    // ===============================

    /**
     * Check if player is rate limited for a specific action
     */
    public boolean isRateLimited(UUID uuid, String action, int maxAttempts, long windowMs) {
        Map<String, RateLimit> limits = playerRateLimits.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        RateLimit limit = limits.computeIfAbsent(action, k -> new RateLimit(maxAttempts, windowMs));

        return !limit.tryAction();
    }

    /**
     * Check if IP is rate limited for a specific action
     */
    public boolean isIPRateLimited(InetAddress ip, String action, int maxAttempts, long windowMs) {
        Map<String, RateLimit> limits = ipRateLimits.computeIfAbsent(ip, k -> new ConcurrentHashMap<>());
        RateLimit limit = limits.computeIfAbsent(action, k -> new RateLimit(maxAttempts, windowMs));

        return !limit.tryAction();
    }

    /**
     * Reset rate limit for player action
     */
    public void resetRateLimit(UUID uuid, String action) {
        Map<String, RateLimit> limits = playerRateLimits.get(uuid);
        if (limits != null) {
            limits.remove(action);
        }
    }

    // ===============================
    // COMMAND COOLDOWNS
    // ===============================

    /**
     * Check if player has command on cooldown
     */
    public boolean isOnCooldown(UUID uuid, String command) {
        Map<String, Long> cooldowns = commandCooldowns.get(uuid);
        if (cooldowns == null) return false;

        Long expiry = cooldowns.get(command);
        if (expiry == null) return false;

        if (expiry < System.currentTimeMillis()) {
            cooldowns.remove(command);
            return false;
        }

        return true;
    }

    /**
     * Set command cooldown for player
     */
    public void setCooldown(UUID uuid, String command, long cooldownMs) {
        Map<String, Long> cooldowns = commandCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        cooldowns.put(command, System.currentTimeMillis() + cooldownMs);
    }

    /**
     * Get remaining cooldown time
     */
    public long getRemainingCooldown(UUID uuid, String command) {
        Map<String, Long> cooldowns = commandCooldowns.get(uuid);
        if (cooldowns == null) return 0;

        Long expiry = cooldowns.get(command);
        if (expiry == null) return 0;

        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    // ===============================
    // IP BLOCKING
    // ===============================

    /**
     * Block an IP address
     */
    public CompletableFuture<Boolean> blockIP(InetAddress ip, String reason, UUID blockedBy, Long expiresAt) {
        return CompletableFuture.supplyAsync(() -> {
            blockedIPs.add(ip);

            if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
                Set<String> blocked = database.getJSONData("blocked_ips", Set.class, new HashSet<>());
                blocked.add(ip.getHostAddress());
                database.setJSONData("blocked_ips", blocked);

                // Store additional data
                Map<String, Object> blockData = new HashMap<>();
                blockData.put("reason", reason);
                blockData.put("blockedBy", blockedBy.toString());
                blockData.put("blockedAt", System.currentTimeMillis());
                if (expiresAt != null) blockData.put("expiresAt", expiresAt);
                database.setJSONData("block_data_" + ip.getHostAddress(), blockData);
            } else {
                database.executeUpdate(
                        "INSERT INTO eclipse_security_blocks (ip, reason, blocked_by, expires_at) VALUES (?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE reason = VALUES(reason), blocked_by = VALUES(blocked_by), expires_at = VALUES(expires_at)",
                        ip.getHostAddress(), reason, blockedBy.toString(),
                        expiresAt != null ? new java.sql.Timestamp(expiresAt) : null
                );
            }

            // Kick all players from this IP
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(player -> Objects.requireNonNull(player.getAddress()).getAddress().equals(ip))
                    .forEach(player -> player.kickPlayer("§cYour IP has been blocked: " + reason));

            plugin.getLogger().info("Blocked IP: " + ip.getHostAddress() + " - Reason: " + reason);
            return true;
        });
    }

    /**
     * Unblock an IP address
     */
    public CompletableFuture<Boolean> unblockIP(InetAddress ip) {
        return CompletableFuture.supplyAsync(() -> {
            boolean removed = blockedIPs.remove(ip);

            if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
                Set<String> blocked = database.getJSONData("blocked_ips", Set.class, new HashSet<>());
                blocked.remove(ip.getHostAddress());
                database.setJSONData("blocked_ips", blocked);
                database.removeJSONData("block_data_" + ip.getHostAddress());
            } else {
                database.executeUpdate("DELETE FROM eclipse_security_blocks WHERE ip = ?", ip.getHostAddress());
            }

            if (removed) {
                plugin.getLogger().info("Unblocked IP: " + ip.getHostAddress());
            }

            return removed;
        });
    }

    /**
     * Check if IP is blocked
     */
    public boolean isIPBlocked(InetAddress ip) {
        return blockedIPs.contains(ip);
    }

    /**
     * Get all blocked IPs
     */
    public Set<InetAddress> getBlockedIPs() {
        return new HashSet<>(blockedIPs);
    }

    // ===============================
    // ANTI-SPAM
    // ===============================

    /**
     * Check if message is spam
     */
    public boolean isSpam(UUID uuid, String message) {
        AntiSpamData data = antiSpamData.computeIfAbsent(uuid, k -> new AntiSpamData());
        return data.isSpam(message, config.getSpamThreshold(), config.getSpamWindowMs());
    }

    /**
     * Record chat message for spam detection
     */
    public void recordChatMessage(UUID uuid, String message) {
        AntiSpamData data = antiSpamData.computeIfAbsent(uuid, k -> new AntiSpamData());
        data.addMessage(message);
    }

    // ===============================
    // SECURITY VIOLATIONS
    // ===============================

    /**
     * Record a security violation
     */
    public void recordViolation(InetAddress ip, UUID uuid, ViolationType type, String details) {
        SecurityViolation violation = new SecurityViolation(ip, uuid, type, details);

        // Store in database
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            List<SecurityViolation> violations = database.getJSONData("security_violations", List.class, new ArrayList<>());
            violations.add(violation);
            database.setJSONData("security_violations", violations);
        } else {
            database.executeUpdate(
                    "INSERT INTO eclipse_security_violations (ip, uuid, violation_type, details) VALUES (?, ?, ?, ?)",
                    ip != null ? ip.getHostAddress() : null,
                    uuid != null ? uuid.toString() : null,
                    type.name(),
                    details
            );
        }

        // Check for automatic actions
        handleViolation(ip, uuid, type, violation);
    }

    /**
     * Handle security violation with automatic actions
     */
    private void handleViolation(InetAddress ip, UUID uuid, ViolationType type, SecurityViolation violation) {
        if (ip != null) {
            SecurityViolation ipViolation = ipViolations.compute(ip, (k, v) -> {
                if (v == null) return violation;
                v.incrementCount();
                return v;
            });

            // Auto-block IP after multiple violations
            if (ipViolation.getCount() >= config.getAutoBlockThreshold()) {
                blockIP(ip, "Automatic block after " + ipViolation.getCount() + " violations",
                        null, System.currentTimeMillis() + config.getAutoBlockDurationMs());
            }
        }

        plugin.getLogger().warning(String.format("Security violation: %s from %s (%s) - %s",
                type, ip != null ? ip.getHostAddress() : "unknown",
                uuid != null ? uuid : "unknown", violation.getDetails()));
    }

    // ===============================
    // EVENT HANDLERS
    // ===============================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        InetAddress ip = event.getAddress();

        if (isIPBlocked(ip)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "§cYour IP address has been blocked.");
            return;
        }

        // Check for login rate limiting
        if (isIPRateLimited(ip, "login", config.getMaxLoginsPerMinute(), 60000)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cToo many login attempts. Please wait.");
            recordViolation(ip, event.getPlayer().getUniqueId(), ViolationType.RATE_LIMIT_EXCEEDED, "Login rate limit");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InetAddress ip = Objects.requireNonNull(player.getAddress()).getAddress();

        // Initialize anti-spam data
        antiSpamData.put(player.getUniqueId(), new AntiSpamData());

        // Log join for monitoring
        plugin.getLogger().info(String.format("Player %s joined from %s",
                player.getName(), ip.getHostAddress()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].substring(1); // Remove the '/'

        // Check command cooldown
        if (isOnCooldown(player.getUniqueId(), command)) {
            long remaining = getRemainingCooldown(player.getUniqueId(), command);
            player.sendMessage("§cCommand on cooldown. Wait " + (remaining / 1000) + " seconds.");
            event.setCancelled(true);
            return;
        }

        // Check command rate limiting
        if (isRateLimited(player.getUniqueId(), "command", config.getMaxCommandsPerSecond(), 1000)) {
            player.sendMessage("§cYou're sending commands too quickly!");
            event.setCancelled(true);
            recordViolation(Objects.requireNonNull(player.getAddress()).getAddress(), player.getUniqueId(),
                    ViolationType.RATE_LIMIT_EXCEEDED, "Command spam");
            return;
        }

        // Set cooldown if configured
        Long cooldown = config.getCommandCooldown(command);
        if (cooldown != null && cooldown > 0) {
            setCooldown(player.getUniqueId(), command, cooldown);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check for spam
        if (isSpam(player.getUniqueId(), message)) {
            player.sendMessage("§cMessage blocked: Spam detected!");
            event.setCancelled(true);
            recordViolation(Objects.requireNonNull(player.getAddress()).getAddress(), player.getUniqueId(),
                    ViolationType.SPAM_DETECTED, "Chat spam: " + message);
            return;
        }

        // Check chat rate limiting
        if (isRateLimited(player.getUniqueId(), "chat", config.getMaxChatPerSecond(), 1000)) {
            player.sendMessage("§cYou're chatting too quickly!");
            event.setCancelled(true);
            recordViolation(Objects.requireNonNull(player.getAddress()).getAddress(), player.getUniqueId(),
                    ViolationType.RATE_LIMIT_EXCEEDED, "Chat rate limit");
            return;
        }

        // Record message for spam detection
        recordChatMessage(player.getUniqueId(), message);
    }

    // ===============================
    // CONFIGURATION AND UTILITIES
    // ===============================

    /**
     * Get security configuration
     */
    public SecurityConfig getConfig() {
        return config;
    }

    /**
     * Get security statistics
     */
    public SecurityStats getStats() {
        return new SecurityStats(
                blockedIPs.size(),
                ipViolations.size(),
                playerRateLimits.size(),
                commandCooldowns.size(),
                antiSpamData.size()
        );
    }

    /**
     * Clean up player data on disconnect
     */
    public void cleanupPlayer(UUID uuid) {
        playerRateLimits.remove(uuid);
        commandCooldowns.remove(uuid);
        antiSpamData.remove(uuid);
    }

    /**
     * Shutdown security manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Security Manager...");

        // Save blocked IPs to ensure persistence
        if (database.getDatabaseType() == DatabaseManager.DatabaseType.JSON) {
            Set<String> blockedIPStrings = new HashSet<>();
            for (InetAddress ip : blockedIPs) {
                blockedIPStrings.add(ip.getHostAddress());
            }
            database.setJSONData("blocked_ips", blockedIPStrings);
        }

        // Clear rate limits
        int playerRateLimitCount = playerRateLimits.size();
        int ipRateLimitCount = ipRateLimits.size();
        playerRateLimits.clear();
        ipRateLimits.clear();

        // Clear command cooldowns
        int cooldownCount = commandCooldowns.values().stream()
                .mapToInt(Map::size)
                .sum();
        commandCooldowns.clear();

        // Clear blocked IPs (keep in database)
        int blockedIPCount = blockedIPs.size();
        blockedIPs.clear();

        // Clear violations
        int violationCount = ipViolations.size();
        ipViolations.clear();

        // Clear anti-spam data
        int antiSpamCount = antiSpamData.size();
        antiSpamData.clear();

        plugin.getLogger().info("Security Manager shutdown complete - Cleared: " +
                playerRateLimitCount + " player rate limits, " +
                ipRateLimitCount + " IP rate limits, " +
                cooldownCount + " cooldowns, " +
                blockedIPCount + " blocked IPs (saved), " +
                violationCount + " violations, " +
                antiSpamCount + " anti-spam entries");
    }

    // ===============================
    // HELPER CLASSES
    // ===============================

    public enum ViolationType {
        RATE_LIMIT_EXCEEDED,
        SPAM_DETECTED,
        SUSPICIOUS_ACTIVITY,
        BLOCKED_IP_ACCESS,
        INVALID_LOGIN
    }

    public static class SecurityStats {
        private final int blockedIPs;
        private final int violationCount;
        private final int activeRateLimits;
        private final int activeCooldowns;
        private final int monitoredPlayers;

        public SecurityStats(int blockedIPs, int violationCount, int activeRateLimits,
                             int activeCooldowns, int monitoredPlayers) {
            this.blockedIPs = blockedIPs;
            this.violationCount = violationCount;
            this.activeRateLimits = activeRateLimits;
            this.activeCooldowns = activeCooldowns;
            this.monitoredPlayers = monitoredPlayers;
        }

        // Getters
        public int getBlockedIPs() { return blockedIPs; }
        public int getViolationCount() { return violationCount; }
        public int getActiveRateLimits() { return activeRateLimits; }
        public int getActiveCooldowns() { return activeCooldowns; }
        public int getMonitoredPlayers() { return monitoredPlayers; }

        @Override
        public String toString() {
            return String.format("SecurityStats{blocked=%d, violations=%d, rateLimits=%d, cooldowns=%d, monitored=%d}",
                    blockedIPs, violationCount, activeRateLimits, activeCooldowns, monitoredPlayers);
        }
    }
}