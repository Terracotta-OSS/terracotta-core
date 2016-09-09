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
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;


public class TerracottaServiceProviderRegistryImpl implements TerracottaServiceProviderRegistry {
  private static final TCLogger logger = TCLogging.getLogger(TerracottaServiceProviderRegistryImpl.class);

  // We need to hold on to the configuration when we are initialized so we can give it to services registered later as
  //  built-ins.
  private final Set<ServiceProvider> serviceProviders = new HashSet<>();
  private final Set<ImplementationProvidedServiceProvider> implementationProvidedServiceProviders = new HashSet<>();

  // In order to prevent ordering errors during start-up, we will set a flag when we hand out sub-registries and make sure that we haven't done that when registering a service.
  private boolean hasCreatedSubRegistries;

  @Override
  public void initialize(String serverName, TcConfiguration configuration, ClassLoader loader) {
    List<ServiceProviderConfiguration> serviceProviderConfigurationList = configuration.getServiceConfigurations().get(serverName);
    Assert.assertFalse(this.hasCreatedSubRegistries);
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
          service.initialize(null);
          registerNewServiceProvider(service);
        }
      } catch (IllegalAccessException | InstantiationException i) {
        logger.error("caught exception while initializing service " + clazz, i);
      }
    }
  }

  @Override
  public  void registerExternal(ServiceProvider service) {
    Assert.assertFalse(this.hasCreatedSubRegistries);
    registerNewServiceProvider(service);
  }

  @Override
  public  void registerImplementationProvided(ImplementationProvidedServiceProvider service) {
    Assert.assertFalse(this.hasCreatedSubRegistries);
    logger.info("Registering implementation-provided service " + service);
    implementationProvidedServiceProviders.add(service);
  }

  @Override
  public DelegatingServiceRegistry subRegistry(long consumerID) {
    this.hasCreatedSubRegistries = true;
    return new DelegatingServiceRegistry(consumerID, serviceProviders.toArray(new ServiceProvider[serviceProviders.size()]), implementationProvidedServiceProviders.toArray(new ImplementationProvidedServiceProvider[implementationProvidedServiceProviders.size()]));
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

    for(ImplementationProvidedServiceProvider builtInServiceProvider : implementationProvidedServiceProviders) {
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

    for (ImplementationProvidedServiceProvider implementationProvidedServiceProvider : implementationProvidedServiceProviders) {
      // ServiceProviders can optionally implement StateDumpable, so we do a instanceof check before calling dump state
      // method
      if(implementationProvidedServiceProvider instanceof StateDumpable) {
        ((StateDumpable) implementationProvidedServiceProvider).dumpStateTo(stateDumper.subStateDumper(implementationProvidedServiceProvider.getClass().getName()));
      }
    }
  }
}
