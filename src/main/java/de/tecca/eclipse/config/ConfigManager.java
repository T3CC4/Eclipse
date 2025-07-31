package de.tecca.eclipse.config;

import de.tecca.eclipse.Eclipse;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;

/**
 * Professional configuration manager for Eclipse Framework
 */
public class ConfigManager {

    private final Plugin plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configs = new ConcurrentHashMap<>();
        this.configFiles = new ConcurrentHashMap<>();
    }

    /**
     * Load a configuration file
     * @param fileName Name of the config file (without .yml)
     * @param createDefault Whether to create default if it doesn't exist
     * @return FileConfiguration instance
     */
    public FileConfiguration loadConfig(String fileName, boolean createDefault) {
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }

        File configFile = new File(plugin.getDataFolder(), fileName);

        if (!configFile.exists() && createDefault) {
            createDefaultConfig(fileName, configFile);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configs.put(fileName, config);
        configFiles.put(fileName, configFile);

        return config;
    }

    /**
     * Get a loaded configuration
     * @param fileName Name of the config file
     * @return FileConfiguration or null if not loaded
     */
    public FileConfiguration getConfig(String fileName) {
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }
        return configs.get(fileName);
    }

    /**
     * Save a configuration file
     * @param fileName Name of the config file
     */
    public void saveConfig(String fileName) {
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }

        FileConfiguration config = configs.get(fileName);
        File configFile = configFiles.get(fileName);

        if (config != null && configFile != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                Eclipse.getInstance().getLogger().severe("Could not save config " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Reload a configuration file
     * @param fileName Name of the config file
     */
    public void reloadConfig(String fileName) {
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }

        File configFile = configFiles.get(fileName);
        if (configFile != null && configFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            configs.put(fileName, config);
        }
    }

    /**
     * Create default configuration from resources
     */
    private void createDefaultConfig(String fileName, File configFile) {
        try {
            configFile.getParentFile().mkdirs();

            InputStream resourceStream = plugin.getResource(fileName);
            if (resourceStream != null) {
                Files.copy(resourceStream, configFile.toPath());
                resourceStream.close();
            } else {
                configFile.createNewFile();
            }
        } catch (IOException e) {
            Eclipse.getInstance().getLogger().severe("Could not create default config " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Get string value with default
     */
    public String getString(String configName, String path, String defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getString(path, defaultValue) : defaultValue;
    }

    /**
     * Get integer value with default
     */
    public int getInt(String configName, String path, int defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getInt(path, defaultValue) : defaultValue;
    }

    /**
     * Get boolean value with default
     */
    public boolean getBoolean(String configName, String path, boolean defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    /**
     * Get double value with default
     */
    public double getDouble(String configName, String path, double defaultValue) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getDouble(path, defaultValue) : defaultValue;
    }

    /**
     * Get string list with default
     */
    public List<String> getStringList(String configName, String path) {
        FileConfiguration config = getConfig(configName);
        return config != null ? config.getStringList(path) : List.of();
    }

    /**
     * Set a value in configuration
     */
    public void set(String configName, String path, Object value) {
        FileConfiguration config = getConfig(configName);
        if (config != null) {
            config.set(path, value);
        }
    }

    /**
     * Save all loaded configurations
     */
    public void saveAll() {
        plugin.getLogger().info("Saving all configurations...");

        int savedCount = 0;
        int errorCount = 0;

        for (String fileName : configs.keySet()) {
            try {
                saveConfig(fileName);
                savedCount++;
            } catch (Exception e) {
                errorCount++;
                plugin.getLogger().severe("Failed to save config " + fileName + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Configuration save complete - Saved: " + savedCount + ", Errors: " + errorCount);
    }

    /**
     * Reload all loaded configurations
     */
    public void reloadAll() {
        for (String fileName : configs.keySet()) {
            reloadConfig(fileName);
        }
    }

    /**
     * Cleanup configuration manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Configuration Manager...");

        // Save all configurations one final time
        saveAll();

        // Clear in-memory configurations
        int configCount = configs.size();
        configs.clear();
        configFiles.clear();

        plugin.getLogger().info("Configuration Manager shutdown complete - " + configCount + " configs cleared");
    }
}