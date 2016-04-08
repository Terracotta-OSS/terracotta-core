package com.tc.objectserver.persistence;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.persistence.IPersistentStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class NullPlatformStorageServiceProvider implements ServiceProvider {
    private final Map<Long, IPersistentStorage> providers = new ConcurrentHashMap<>();


    @Override
    public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration) {
        return true;
    }

    @Override
    public <T> T getService(long entityID, ServiceConfiguration<T> serviceConfiguration) {
        providers.putIfAbsent(entityID, new NullPlatformPersistentStorage());
        return serviceConfiguration.getServiceType().cast(providers.get(entityID));
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
        return Collections.singleton(IPersistentStorage.class);
    }

    public void close() throws IOException {
        providers.clear();
    }

    @Override
    public void clear() throws ServiceProviderCleanupException {
        providers.clear();
    }
}
