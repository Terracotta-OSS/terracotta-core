package org.terracotta.passthrough;

import java.util.Map;

import org.terracotta.entity.Service;
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
  public <T> Service<T> getService(ServiceConfiguration<T> configuration) {
    Service<T> service = null;
    ServiceProvider provider = this.serviceProviderMap.get(configuration.getServiceType());
    if (null != provider) {
      service = provider.getService(this.consumerID, configuration);
    }
    return service;
  }

  @Override
  public void destroy() {
    // TODO Auto-generated method stub

  }
}
