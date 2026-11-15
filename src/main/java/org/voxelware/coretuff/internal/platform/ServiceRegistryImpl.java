package org.voxelware.coretuff.internal.platform;

import org.voxelware.coretuff.api.platform.ServiceRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistryImpl implements ServiceRegistry {

    private final Map<Class<?>, RegisteredService<?>> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> serviceClass, T service, ServicePriority priority) {
        services.put(serviceClass, new RegisteredService<>(serviceClass, service, priority));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<T> serviceClass) {
        RegisteredService<?> rs = services.get(serviceClass);
        return rs != null ? (T) rs.service() : null;
    }

    @Override
    public <T> T require(Class<T> serviceClass) {
        T service = get(serviceClass);
        if (service == null) {
            throw new IllegalStateException("Required service not registered: " + serviceClass.getName());
        }
        return service;
    }

    @Override
    public boolean isRegistered(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }

    private record RegisteredService<T>(Class<T> type, T service, ServicePriority priority) {}
}
