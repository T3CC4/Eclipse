package de.tecca.eclipse.database;

import de.tecca.eclipse.api.database.Transaction;
import org.bukkit.plugin.Plugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;

public class JSONProvider implements DatabaseProvider {

    private final Plugin plugin;
    private final File dataDirectory;
    private final Gson gson;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public JSONProvider(Plugin plugin) {
        this.plugin = plugin;
        this.dataDirectory = new File(plugin.getDataFolder(), "json-data");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();

        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        loadAllData();
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... params) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... params) {
        return CompletableFuture.completedFuture(0);
    }

    @Override
    public CompletableFuture<Void> execute(String sql, Object... params) {
        return CompletableFuture.completedFuture(null);
    }

    public <T> CompletableFuture<Void> setJSON(String key, T data) {
        return CompletableFuture.runAsync(() -> {
            cache.put(key, data);
            saveToFile(key, data);
        });
    }

    public <T> CompletableFuture<Optional<T>> getJSON(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            Object data = cache.get(key);
            if (data == null) {
                data = loadFromFile(key, type);
                if (data != null) {
                    cache.put(key, data);
                }
            }

            if (data != null && type.isAssignableFrom(data.getClass())) {
                return Optional.of(type.cast(data));
            }

            return Optional.empty();
        });
    }

    public CompletableFuture<Void> removeJSON(String key) {
        return CompletableFuture.runAsync(() -> {
            cache.remove(key);
            deleteFile(key);
        });
    }

    public CompletableFuture<List<String>> listJSONKeys(String prefix) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> keys = new ArrayList<>();

            for (String key : cache.keySet()) {
                if (key.startsWith(prefix)) {
                    keys.add(key);
                }
            }

            // Also check files
            File[] files = dataDirectory.listFiles((dir, name) ->
                    name.startsWith(prefix) && name.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String key = fileName.substring(0, fileName.length() - 5); // Remove .json
                    if (!keys.contains(key)) {
                        keys.add(key);
                    }
                }
            }

            return keys;
        });
    }

    @Override
    public Transaction beginTransaction() {
        throw new UnsupportedOperationException("Transactions not supported with JSON storage");
    }

    @Override
    public boolean isConnected() {
        return dataDirectory.exists() && dataDirectory.canWrite();
    }

    @Override
    public void shutdown() {
        saveAllData();
        cache.clear();
    }

    private void loadAllData() {
        File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String key = file.getName().substring(0, file.getName().length() - 5);
                Object data = loadFromFile(key, Object.class);
                if (data != null) {
                    cache.put(key, data);
                }
            }
        }
    }

    private void saveAllData() {
        for (Map.Entry<String, Object> entry : cache.entrySet()) {
            saveToFile(entry.getKey(), entry.getValue());
        }
    }

    private void saveToFile(String key, Object data) {
        File file = new File(dataDirectory, sanitizeKey(key) + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save JSON data for key " + key + ": " + e.getMessage());
        }
    }

    private <T> T loadFromFile(String key, Class<T> type) {
        File file = new File(dataDirectory, sanitizeKey(key) + ".json");

        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load JSON data for key " + key + ": " + e.getMessage());
            return null;
        }
    }

    private void deleteFile(String key) {
        File file = new File(dataDirectory, sanitizeKey(key) + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    private String sanitizeKey(String key) {
        return key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}