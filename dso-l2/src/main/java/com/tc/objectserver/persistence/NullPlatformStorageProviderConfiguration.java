package com.tc.objectserver.persistence;

import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 * @author vmad
 */
public class NullPlatformStorageProviderConfiguration implements ServiceProviderConfiguration {
    @Override
    public Class<? extends ServiceProvider> getServiceProviderType() {
        return NullPlatformStorageServiceProvider.class;
    }
}
