package org.terracotta.passthrough;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.monitoring.PlatformService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author vmad
 */
public class PassthroughPlatformServiceProvider implements PassthroughBuiltInServiceProvider {
    private final PassthroughPlatformService passthroughPlatformService;

    public PassthroughPlatformServiceProvider(PassthroughDumper passthroughDumper) {
        this.passthroughPlatformService = new PassthroughPlatformService(passthroughDumper);
    }


    @Override
    public <T> T getService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration) {
        return configuration.getServiceType().cast(passthroughPlatformService);
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(PlatformService.class);
        return set;
    }
}
