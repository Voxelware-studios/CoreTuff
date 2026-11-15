package org.voxelware.coretuff.api.platform;

public interface ServiceRegistry {

    <T> void register(Class<T> serviceClass, T service, ServicePriority priority);

    <T> T get(Class<T> serviceClass);

    <T> T require(Class<T> serviceClass);

    boolean isRegistered(Class<?> serviceClass);

    enum ServicePriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST
    }
}
