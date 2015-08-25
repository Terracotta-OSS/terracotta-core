package com.tc.services;

import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.ServiceRegistry;

import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;

/**
 * Platform level service provider registry which has instances of all the service provider at the platform level.
 *
 * @author twu
 */
public interface TerracottaServiceProviderRegistry {

  /**
   * Initialize each of the service provider with platform configuration
   *
   * @param configuration platform configuration which each service provider can query for their service configuration
   *
   */
  void initialize(String serverName, TcConfiguration configuration);

  /**
   * Method to register platform level service provider which don't have life-cycle using SPI interface.
   * Note that this serviceProvider will also be initialized with the same configuration used to initialize the registry.
   *
   * @param serviceProvider platform service provider
   */
  void registerBuiltin(ServiceProvider serviceProvider);

  /**
   * Creates a entity level service registry which has list of service instances managed by the service providers
   *
   * @param consumerID The unique ID which will be used to name-space services created by this sub-registry
   * @return Service registry which will be used by entities.
   */
  ServiceRegistry subRegistry(long consumerID);

  /**
   * Returns an instance of service provider given an a class. This method is not to be used by entities but by platform
   * to provision resources for it.
   *
   * @param consumerId
   * @param config service provider type
   * @param <T> service type
   * @return instance of service
   */
  <T> Service<T> getService(long consumerId, ServiceConfiguration<T> config);
}
