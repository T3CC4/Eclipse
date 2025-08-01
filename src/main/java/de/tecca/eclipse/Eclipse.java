package de.tecca.eclipse;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import de.tecca.eclipse.api.*;
import de.tecca.eclipse.impl.*;
import de.tecca.eclipse.core.*;
import de.tecca.eclipse.psp.PSPManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Eclipse extends JavaPlugin {

    private static Eclipse instance;
    private static boolean initialized = false;
    private static boolean isStandaloneMode = false;

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    private DatabaseAPI database;
    private EventAPI events;
    private TaskAPI tasks;
    private PermissionAPI permissions;

    private PSPManager pspManager;
    private EclipseCommandManager commandManager;
    private ServiceRegistry serviceRegistry;

    @Override
    public void onEnable() {
        isStandaloneMode = true;
        instance = this;

        try {
            initializeServices();
            initializePSPSystem();
            initializeCommands();
            initialized = true;

            getLogger().info("Eclipse Framework started successfully!");

        } catch (Exception e) {
            getLogger().severe("Failed to start Eclipse Framework: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        performShutdown();
    }

    public static synchronized Eclipse initialize(Plugin plugin) {
        if (instance != null) {
            return instance;
        }

        instance = new Eclipse();
        instance.initializeAsLibrary(plugin);
        initialized = true;

        return instance;
    }

    public static synchronized Eclipse initializeWithPSP(Plugin plugin) {
        Eclipse eclipse = initialize(plugin);

        if (!isStandaloneMode) {
            eclipse.enablePSPSystem(plugin);
        }

        return eclipse;
    }

    private void initializeServices() {
        Plugin contextPlugin = isStandaloneMode ? this : getHostPlugin();

        serviceRegistry = new ServiceRegistry();

        database = new DatabaseAPIImpl(contextPlugin);
        events = new EventAPIImpl(contextPlugin);
        tasks = new TaskAPIImpl(contextPlugin);
        permissions = new PermissionAPIImpl(contextPlugin);

        registerService(DatabaseAPI.class, database);
        registerService(EventAPI.class, events);
        registerService(TaskAPI.class, tasks);
        registerService(PermissionAPI.class, permissions);
    }

    private void initializeAsLibrary(Plugin hostPlugin) {
        initializeServices();
    }

    private void initializePSPSystem() {
        enablePSPSystem(this);
    }

    private void enablePSPSystem(Plugin hostPlugin) {
        if (pspManager == null) {
            pspManager = new PSPManager(hostPlugin);
            pspManager.initialize();
            registerService(PSPManager.class, pspManager);
        }
    }

    private void initializeCommands() {
        if (isStandaloneMode) {
            commandManager = new EclipseCommandManager(this);
            commandManager.registerCommands();
        }
    }

    private void performShutdown() {
        if (pspManager != null) pspManager.shutdown();
        if (commandManager != null) commandManager.shutdown();
        if (permissions != null) permissions.shutdown();
        if (tasks != null) tasks.shutdown();
        if (events != null) events.shutdown();
        if (database != null) database.shutdown();

        instance = null;
        initialized = false;
        isStandaloneMode = false;
    }

    private Plugin getHostPlugin() {
        return getServer().getPluginManager().getPlugins()[0];
    }

    public <T> void registerService(Class<T> serviceClass, T implementation) {
        services.put(serviceClass, implementation);
    }

    public <T> T getService(Class<T> serviceClass) {
        return serviceClass.cast(services.get(serviceClass));
    }

    public static Eclipse getInstance() {
        if (!initialized) {
            throw new IllegalStateException("Eclipse Framework not initialized!");
        }
        return instance;
    }

    public static boolean isStandaloneMode() { return isStandaloneMode; }
    public static boolean isLibraryMode() { return initialized && !isStandaloneMode; }
    public static boolean isInitialized() { return initialized; }

    public static DatabaseAPI getDatabase() { return getInstance().database; }
    public static EventAPI getEvents() { return getInstance().events; }
    public static TaskAPI getTasks() { return getInstance().tasks; }
    public static PermissionAPI getPermissions() { return getInstance().permissions; }

    public static PSPManager getPSP() {
        PSPManager manager = getInstance().pspManager;
        if (manager == null) {
            throw new IllegalStateException("PSP System not enabled!");
        }
        return manager;
    }

    public static EclipseCommandManager getCommandManager() {
        if (!isStandaloneMode) {
            throw new IllegalStateException("Command Manager only available in standalone mode");
        }
        return getInstance().commandManager;
    }

    public static String getVersion() { return "1.0.0"; }

    public static void shutdown() {
        if (instance != null && !isStandaloneMode) {
            getInstance().performShutdown();
        }
    }
}