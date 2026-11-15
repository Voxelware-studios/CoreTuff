package org.voxelware.coretuff.api.event;

import org.voxelware.coretuff.api.platform.ServiceRegistry;

public class ServiceRegistrationEvent extends CoreTuffEvent {

    private final Class<?> serviceClass;
    private final Object service;
    private final ServiceRegistry.ServicePriority priority;

    public ServiceRegistrationEvent(Class<?> serviceClass, Object service, ServiceRegistry.ServicePriority priority) {
        this.serviceClass = serviceClass;
        this.service = service;
        this.priority = priority;
    }

    public Class<?> serviceClass() { return serviceClass; }
    public Object service() { return service; }
    public ServiceRegistry.ServicePriority priority() { return priority; }
}
