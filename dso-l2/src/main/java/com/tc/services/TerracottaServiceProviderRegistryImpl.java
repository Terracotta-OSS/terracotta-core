package com.tc.services;

import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.ServiceRegistry;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;

/**
 * @author twu
 */
public class TerracottaServiceProviderRegistryImpl implements TerracottaServiceProviderRegistry {
  private static final TCLogger logger = TCLogging.getLogger(TerracottaServiceProviderRegistryImpl.class);

  // We need to hold on to the configuration when we are initialized so we can give it to services registered later as
  //  built-ins.
  private final Set<ServiceProvider> serviceProviders = new HashSet<>();

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(String serverName, TcConfiguration configuration) {
    for (ServiceProviderConfiguration config : configuration.getServiceConfigurations().get(serverName)) {
      Class<? extends ServiceProvider> serviceClazz = config.getServiceProviderType();
      try {
        ServiceProvider provider = serviceClazz.newInstance();
        if (provider.initialize(config)) {
          registerNewServiceProvider(provider);
        }
      } catch (InstantiationException | IllegalAccessException ie) {
//  really shouldn't be doing this.  ServiceProvider configurations should provide appropriate classes
//  to handle the config.
        tryServiceLoader(config);
      }

    }
  }
  
  private void tryServiceLoader(ServiceProviderConfiguration config) {
    boolean initialized = false;
    for (ServiceProvider service : ServiceLoader.load(config.getServiceProviderType())) {
      if (service.initialize(config)) {
        if (initialized) {
          throw new AssertionError("double initialization");
        }
        registerNewServiceProvider(service);
        initialized = true;
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public  void registerBuiltin(ServiceProvider service) {
    registerNewServiceProvider(service);
  }

  @Override
  public ServiceRegistry subRegistry(long consumerID) {
    return new DelegatingServiceRegistry(consumerID, serviceProviders.toArray(new ServiceProvider[serviceProviders.size()]));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Service<T> getService(long consumerId, ServiceConfiguration<T> config) {
    for(ServiceProvider provider : serviceProviders) {
        Service s = provider.getService(consumerId, config);
        if (s != null) {
          return s;
        }
    }
    return null;
  }

  private void registerNewServiceProvider(ServiceProvider service) {
    logger.info("Initializing " + service);
    serviceProviders.add(service);
  }
}
