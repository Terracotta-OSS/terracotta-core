package com.tc.objectserver.persistence;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.persistence.IPersistentStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class NullPlatformStorageServiceProvider implements ServiceProvider, StateDumpable {
    private final Map<Long, NullPlatformPersistentStorage> providers = new ConcurrentHashMap<>();


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

    @Override
    public void dumpStateTo(StateDumper stateDumper) {
        for (Map.Entry<Long, NullPlatformPersistentStorage> entry : providers.entrySet()) {
            entry.getValue().dumpStateTo(stateDumper.subStateDumper(String.valueOf(entry.getKey())));
        }
    }
}
