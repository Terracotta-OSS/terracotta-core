package org.terracotta.passthrough;

import java.util.Collection;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;


/**
 * The provider of PassthroughCommunicatorService, to server-side entities.  It has no meaningful implementation beyond
 * providing that.
 */
public class PassthroughCommunicatorServiceProvider implements ServiceProvider {
  @Override
  public void close() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    return configuration.getServiceType().cast(new PassthroughCommunicatorService());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    // TODO Auto-generated method stub
    return null;
  }
}
