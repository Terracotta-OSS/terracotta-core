package org.terracotta.passthrough;

import java.util.Collection;

import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;


public class PassthroughCommunicatorServiceProvider implements ServiceProvider {

  @Override
  public void close() throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    // TODO Auto-generated method stub
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Service<T> getService(long consumerID, ServiceConfiguration<T> configuration) {
    return (Service<T>) new PassthroughCommunicatorService();
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    // TODO Auto-generated method stub
    return null;
  }


}
