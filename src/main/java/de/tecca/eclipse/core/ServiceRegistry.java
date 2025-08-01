package de.tecca.eclipse.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistry {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final Map<String, Object> namedServices = new ConcurrentHashMap<>();

    public <T> void register(Class<T> serviceClass, T implementation) {
        services.put(serviceClass, implementation);
    }

    public <T> T get(Class<T> serviceClass) {
        return serviceClass.cast(services.get(serviceClass));
    }

    public void registerNamed(String name, Object service) {
        namedServices.put(name, service);
    }

    public <T> T getNamed(String name, Class<T> type) {
        return type.cast(namedServices.get(name));
    }

    public boolean hasService(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }

    public boolean hasNamedService(String name) {
        return namedServices.containsKey(name);
    }

    public void unregister(Class<?> serviceClass) {
        services.remove(serviceClass);
    }

    public void unregisterNamed(String name) {
        namedServices.remove(name);
    }

    public void clear() {
        services.clear();
        namedServices.clear();
    }

    public int getServiceCount() {
        return services.size() + namedServices.size();
    }
}
