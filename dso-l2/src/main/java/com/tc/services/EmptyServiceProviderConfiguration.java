package com.tc.services;

import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 * Configuration for service providers which don't need special configuration to initialize
 */
public class EmptyServiceProviderConfiguration implements ServiceProviderConfiguration {
  
  private final Class<? extends ServiceProvider> clazz;

  public EmptyServiceProviderConfiguration(Class<? extends ServiceProvider> clazz) {
    this.clazz = clazz;
  }

  @Override
  public Class<? extends ServiceProvider> getServiceProviderType() {
    return clazz;
  }
  
  
}
