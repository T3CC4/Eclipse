package de.tecca.eclipse.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationManager {

    private final Plugin plugin;
    private final Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();

    public ConfigurationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);

        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configs.put(fileName, config);

        return config;
    }

    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        if (config != null) {
            try {
                config.save(new File(plugin.getDataFolder(), fileName));
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save " + fileName + ": " + e.getMessage());
            }
        }
    }

    public void reloadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configs.put(fileName, config);
    }

    public void reloadAllConfigs() {
        for (String fileName : configs.keySet()) {
            reloadConfig(fileName);
        }
    }

    public boolean hasConfig(String fileName) {
        return configs.containsKey(fileName);
    }

    public void removeConfig(String fileName) {
        configs.remove(fileName);
    }

    public int getConfigCount() {
        return configs.size();
    }
}