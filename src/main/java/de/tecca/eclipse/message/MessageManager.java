package de.tecca.eclipse.message;

import de.tecca.eclipse.Eclipse;
import de.tecca.eclipse.config.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Professional message and localization manager
 */
public class MessageManager {

    private final Eclipse plugin;
    private final ConfigManager configManager;
    private final Map<String, String> messages;
    private final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");

    public MessageManager(Eclipse plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messages = new ConcurrentHashMap<>();
        loadMessages();
    }

    /**
     * Load messages from configuration
     */
    public void loadMessages() {
        messages.clear();

        // Load messages.yml
        var config = configManager.loadConfig("messages", true);

        // Load all message keys
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }

        plugin.getLogger().info("Loaded " + messages.size() + " messages");
    }

    /**
     * Get a message by key
     * @param key Message key
     * @return Formatted message or key if not found
     */
    public String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Get a message with placeholders
     * @param key Message key
     * @param placeholders Placeholder replacements
     * @return Formatted message
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return colorize(message);
    }

    /**
     * Get a message with simple placeholder replacement
     * @param key Message key
     * @param placeholder Placeholder name
     * @param value Replacement value
     * @return Formatted message
     */
    public String getMessage(String key, String placeholder, String value) {
        return getMessage(key, Map.of(placeholder, value));
    }

    /**
     * Send a message to a command sender
     * @param sender Command sender
     * @param key Message key
     */
    public void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(colorize(getMessage(key)));
    }

    /**
     * Send a message with placeholders to a command sender
     * @param sender Command sender
     * @param key Message key
     * @param placeholders Placeholder replacements
     */
    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(getMessage(key, placeholders));
    }

    /**
     * Send a message with simple placeholder to a command sender
     * @param sender Command sender
     * @param key Message key
     * @param placeholder Placeholder name
     * @param value Replacement value
     */
    public void sendMessage(CommandSender sender, String key, String placeholder, String value) {
        sendMessage(sender, key, Map.of(placeholder, value));
    }

    /**
     * Send an action bar message to a player
     * @param player Player
     * @param key Message key
     */
    public void sendActionBar(Player player, String key) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacy(colorize(getMessage(key))));
    }

    /**
     * Send an action bar message with placeholders to a player
     * @param player Player
     * @param key Message key
     * @param placeholders Placeholder replacements
     */
    public void sendActionBar(Player player, String key, Map<String, String> placeholders) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacy(getMessage(key, placeholders)));
    }

    /**
     * Send a title to a player
     * @param player Player
     * @param titleKey Title message key
     * @param subtitleKey Subtitle message key
     * @param fadeIn Fade in time in ticks
     * @param stay Stay time in ticks
     * @param fadeOut Fade out time in ticks
     */
    public void sendTitle(Player player, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut) {
        String title = titleKey != null ? colorize(getMessage(titleKey)) : "";
        String subtitle = subtitleKey != null ? colorize(getMessage(subtitleKey)) : "";
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Send a title with placeholders to a player
     * @param player Player
     * @param titleKey Title message key
     * @param subtitleKey Subtitle message key
     * @param placeholders Placeholder replacements
     * @param fadeIn Fade in time in ticks
     * @param stay Stay time in ticks
     * @param fadeOut Fade out time in ticks
     */
    public void sendTitle(Player player, String titleKey, String subtitleKey, Map<String, String> placeholders,
                          int fadeIn, int stay, int fadeOut) {
        String title = titleKey != null ? getMessage(titleKey, placeholders) : "";
        String subtitle = subtitleKey != null ? getMessage(subtitleKey, placeholders) : "";
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Broadcast a message to all players
     * @param key Message key
     */
    public void broadcast(String key) {
        plugin.getServer().broadcastMessage(colorize(getMessage(key)));
    }

    /**
     * Broadcast a message with placeholders to all players
     * @param key Message key
     * @param placeholders Placeholder replacements
     */
    public void broadcast(String key, Map<String, String> placeholders) {
        plugin.getServer().broadcastMessage(getMessage(key, placeholders));
    }

    /**
     * Broadcast a message to players with permission
     * @param key Message key
     * @param permission Required permission
     */
    public void broadcast(String key, String permission) {
        String message = colorize(getMessage(key));
        plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> player.sendMessage(message));
    }

    /**
     * Colorize a message (supports both legacy and hex colors)
     * @param message Message to colorize
     * @return Colorized message
     */
    public String colorize(String message) {
        if (message == null) return "";

        // Handle hex colors (#RRGGBB)
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String hexCode = matcher.group();
            try {
                ChatColor color = ChatColor.of(hexCode);
                message = message.replace(hexCode, color.toString());
            } catch (IllegalArgumentException ignored) {
                // Invalid hex code, ignore
            }
        }

        // Handle legacy color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Strip all colors from a message
     * @param message Message to strip
     * @return Message without colors
     */
    public String stripColors(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(colorize(message));
    }

    /**
     * Set a message programmatically
     * @param key Message key
     * @param message Message content
     */
    public void setMessage(String key, String message) {
        messages.put(key, message);
    }

    /**
     * Check if a message key exists
     * @param key Message key
     * @return true if exists
     */
    public boolean hasMessage(String key) {
        return messages.containsKey(key);
    }

    /**
     * Get all loaded messages
     * @return Map of all messages
     */
    public Map<String, String> getAllMessages() {
        return new ConcurrentHashMap<>(messages);
    }

    /**
     * Reload messages from configuration
     */
    public void reload() {
        plugin.getLogger().info("Reloading Message Manager...");

        // Save current message count for comparison
        int oldMessageCount = messages.size();

        try {
            // Reload the messages configuration
            configManager.reloadConfig("messages");

            // Clear and reload messages
            loadMessages();

            int newMessageCount = messages.size();
            plugin.getLogger().info("Message Manager reloaded - Old: " + oldMessageCount + ", New: " + newMessageCount);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload messages: " + e.getMessage());
            // Keep old messages on error
        }
    }

    /**
     * Shutdown message manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Message Manager...");

        int messageCount = messages.size();
        messages.clear();

        plugin.getLogger().info("Message Manager shutdown complete - " + messageCount + " messages cleared");
    }
}