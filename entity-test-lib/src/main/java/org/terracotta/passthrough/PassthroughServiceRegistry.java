package org.terracotta.passthrough;

import java.util.Map;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceRegistry;


/**
 * The registry of services available on a PassthroughServer.
 */
public class PassthroughServiceRegistry implements ServiceRegistry {
  private final long consumerID;
  private final Map<Class<?>, ServiceProvider> serviceProviderMap;
  
  public PassthroughServiceRegistry(long consumerID, Map<Class<?>, ServiceProvider> serviceProviderMap) {
    this.consumerID = consumerID;
    this.serviceProviderMap = serviceProviderMap;
  }

  @Override
  public <T> T getService(ServiceConfiguration<T> configuration) {
    ServiceProvider provider = this.serviceProviderMap.get(configuration.getServiceType());
    T service = null;
    if (null != provider) {
      service = provider.getService(this.consumerID, configuration);
    }
    return service;
  }
}
