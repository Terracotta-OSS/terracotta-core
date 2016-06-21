/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.services;

import com.tc.classloader.BuiltinService;
import com.tc.classloader.ServiceLocator;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.api.ManagedEntity;
import java.util.Collection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;


public class TerracottaServiceProviderRegistryImpl implements TerracottaServiceProviderRegistry {
  private static final TCLogger logger = TCLogging.getLogger(TerracottaServiceProviderRegistryImpl.class);

  // We need to hold on to the configuration when we are initialized so we can give it to services registered later as
  //  built-ins.
  private final Set<ServiceProvider> serviceProviders = new HashSet<>();
  private final Set<BuiltInServiceProvider> builtInServiceProviders = new HashSet<>();

  @Override
  public void initialize(String serverName, TcConfiguration configuration, ClassLoader loader) {
    List<ServiceProviderConfiguration> serviceProviderConfigurationList = configuration.getServiceConfigurations().get(serverName);
    loadClasspathBuiltins(loader);
    if(serviceProviderConfigurationList != null) {
      for (ServiceProviderConfiguration config : serviceProviderConfigurationList) {
        Class<? extends ServiceProvider> serviceClazz = config.getServiceProviderType();
        try {
          ServiceProvider provider = serviceClazz.newInstance();
          if (provider.initialize(config)) {
            registerNewServiceProvider(provider);
          }
        } catch (InstantiationException | IllegalAccessException ie) {
          logger.error("caught exception while initializing service " + serviceClazz, ie);
          throw new RuntimeException(ie);
        }

      }
    }
  }
  
  private void loadClasspathBuiltins(ClassLoader loader) {
    List<Class<? extends ServiceProvider>> providers = ServiceLocator.getImplementations(ServiceProvider.class, loader);
    for (Class<? extends ServiceProvider> clazz : providers) {
      try {
        if (!clazz.isAnnotationPresent(BuiltinService.class)) {
          logger.warn("service:" + clazz.getName() + " is registered as a builtin but is not properly annotated with @BuiltinService.  This builtin will not be loaded");
        } else {
          ServiceProvider service = clazz.newInstance();
    //  there is no config for builtins
          registerBuiltin(new WrappingBuiltinServiceProvider(service));
        }
      } catch (IllegalAccessException | InstantiationException i) {
        logger.error("caught exception while initializing service " + clazz, i);
      }
    }
  }

  @Override
  public  void registerExternal(ServiceProvider service) {
    registerNewServiceProvider(service);
  }

  @Override
  public  void registerBuiltin(BuiltInServiceProvider service) {
    logger.info("Registering built-in service " + service);
    builtInServiceProviders.add(service);
  }

  @Override
  public DelegatingServiceRegistry subRegistry(long consumerID) {
    return new DelegatingServiceRegistry(consumerID, serviceProviders.toArray(new ServiceProvider[serviceProviders.size()]), builtInServiceProviders.toArray(new BuiltInServiceProvider[builtInServiceProviders.size()]));
  }

  @Override
  public <T> T getService(long consumerId, ServiceConfiguration<T> config) {
    for(ServiceProvider provider : serviceProviders) {
        T s = provider.getService(consumerId, config);
        if (s != null) {
          return s;
        }
    }
    return null;
  }

  @Override
  public void clearServiceProvidersState() {
    for(ServiceProvider serviceProvider : serviceProviders) {
      try {
        serviceProvider.clear();
      } catch (ServiceProviderCleanupException e) {
        throw new RuntimeException(e);
      }
    }

    for(BuiltInServiceProvider builtInServiceProvider : builtInServiceProviders) {
      try {
        builtInServiceProvider.clear();
      } catch (ServiceProviderCleanupException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void registerNewServiceProvider(ServiceProvider service) {
    logger.info("Initializing " + service);
    serviceProviders.add(service);
  }

  @Override
  public void dumpStateTo(StateDumper stateDumper) {
    for (ServiceProvider serviceProvider : serviceProviders) {
      // ServiceProviders can optionally implement StateDumpable, so we do a instanceof check before calling dump state
      // method
      if(serviceProvider instanceof StateDumpable) {
        ((StateDumpable) serviceProvider).dumpStateTo(stateDumper.subStateDumper(serviceProvider.getClass().getName()));
      }
    }

    for (BuiltInServiceProvider builtInServiceProvider : builtInServiceProviders) {
      // ServiceProviders can optionally implement StateDumpable, so we do a instanceof check before calling dump state
      // method
      if(builtInServiceProvider instanceof StateDumpable) {
        ((StateDumpable) builtInServiceProvider).dumpStateTo(stateDumper.subStateDumper(builtInServiceProvider
          .getClass()
                                                                                    .getName()));
      }
    }
  }
  
  private class WrappingBuiltinServiceProvider implements BuiltInServiceProvider {
    
    private final ServiceProvider delegate;

    public WrappingBuiltinServiceProvider(ServiceProvider delegate) {
      this.delegate = delegate;
    }

    @Override
    public <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
      return delegate.getService(consumerID, configuration);
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
      return delegate.getProvidedServiceTypes();
    }

    @Override
    public void clear() throws ServiceProviderCleanupException {
      delegate.clear();
    }
    
  }
}
