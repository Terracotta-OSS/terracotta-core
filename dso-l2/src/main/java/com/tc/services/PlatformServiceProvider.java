package com.tc.services;

import com.tc.management.beans.TCDumper;
import com.tc.objectserver.api.ManagedEntity;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.StateDumper;
import org.terracotta.monitoring.PlatformService;

import java.util.Collection;
import java.util.Collections;


public class PlatformServiceProvider implements ImplementationProvidedServiceProvider {

    private final PlatformServiceImpl platformService;

    public PlatformServiceProvider(TCDumper tcDumper) {
        this.platformService = new PlatformServiceImpl(tcDumper);
    }


    @Override
    public <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
        return configuration.getServiceType().cast(platformService);
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
        return Collections.singleton(PlatformService.class);
    }

    @Override
    public void clear() throws ServiceProviderCleanupException {
        //no-op
    }

    @Override
    public void serverDidBecomeActive() {
      // The platform service works the same way whether active or passive - ignore.
    }

    @Override
    public void dumpStateTo(final StateDumper stateDumper) {
        //nothing to do
    }
}
