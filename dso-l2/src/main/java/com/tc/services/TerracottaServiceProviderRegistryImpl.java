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
import com.tc.server.TCServerImpl;
import com.tc.server.TCServerMain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class TerracottaServiceProviderRegistryImpl implements TerracottaServiceProviderRegistry {
  private static final Logger logger = LoggerFactory.getLogger(TerracottaServiceProviderRegistryImpl.class);

  // We need to hold on to the configuration when we are initialized so we can give it to services registered later as
  //  built-ins.
  private final Set<ServiceProvider> serviceProviders = new LinkedHashSet<>();
  private final Set<ImplementationProvidedServiceProvider> implementationProvidedServiceProviders = new LinkedHashSet<>();

  // In order to prevent ordering errors during start-up, we will set a flag when we hand out sub-registries and make sure that we haven't done that when registering a service.
  private boolean hasCreatedSubRegistries;

  @Override
  public void initialize(PlatformConfiguration platformConfiguration, TcConfiguration configuration, ClassLoader loader) {
    List<ServiceProviderConfiguration> serviceProviderConfigurationList = configuration.getServiceConfigurations();
    Assert.assertFalse(this.hasCreatedSubRegistries);
    if(serviceProviderConfigurationList != null) {
      for (ServiceProviderConfiguration config : serviceProviderConfigurationList) {
        Class<? extends ServiceProvider> serviceClazz = config.getServiceProviderType();
        try {
          ServiceProvider provider = serviceClazz.newInstance();
          if (provider.initialize(config, platformConfiguration)) {
            registerNewServiceProvider(provider);
          }
        } catch (InstantiationException | IllegalAccessException ie) {
          logger.error("caught exception while initializing service " + serviceClazz, ie);
          throw new RuntimeException(ie);
        }
      }
    }
    loadClasspathBuiltins(loader, platformConfiguration);
  }
  
  private void loadClasspathBuiltins(ClassLoader loader, PlatformConfiguration platformConfiguration) {
    List<Class<? extends ServiceProvider>> providers = TCServerMain.getSetupManager().getServiceLocator().getImplementations(ServiceProvider.class, loader);
    for (Class<? extends ServiceProvider> clazz : providers) {
      try {
        if (!clazz.isAnnotationPresent(BuiltinService.class)) {
          logger.warn("service:" + clazz.getName() + " is registered as a builtin but is not properly annotated with @BuiltinService.  This builtin will not be loaded");
        } else {
          // only add a builtin if one has not already been configured into the system via xml
          if (serviceProviders.stream().noneMatch(sp->sp.getClass().getName().equals(clazz.getName()))) {
            ServiceProvider service = clazz.newInstance();
            //  there is no config for builtins
            if (service.initialize(null, platformConfiguration)) {
              registerNewServiceProvider(service);
            }
          }
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
    if (consumerID > 0) {
      this.hasCreatedSubRegistries = true;
    }
    return new DelegatingServiceRegistry(consumerID, serviceProviders.toArray(new ServiceProvider[serviceProviders.size()]), implementationProvidedServiceProviders.toArray(new ImplementationProvidedServiceProvider[implementationProvidedServiceProviders.size()]));
  }

  @Override
  public void clearServiceProvidersState() {
    for(ServiceProvider serviceProvider : serviceProviders) {
      try {
        serviceProvider.prepareForSynchronization();
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

  @Override
  public void notifyServerDidBecomeActive() {
    for(ImplementationProvidedServiceProvider builtInServiceProvider : implementationProvidedServiceProviders) {
      builtInServiceProvider.serverDidBecomeActive();
    }
  }

  private void registerNewServiceProvider(ServiceProvider service) {
    logger.info("Initializing " + service);
    serviceProviders.add(service);
  }

  /**
   * @return True if there is a user-provided service for the given class registered.
   */
  public boolean hasUserProvidedServiceProvider(Class<?> serviceInterface) {
    boolean hasProvider = false;
    for (ServiceProvider serviceProvider : this.serviceProviders) {
      if (serviceProvider.getProvidedServiceTypes().contains(serviceInterface)) {
        hasProvider = true;
        break;
      }
    }
    return hasProvider;
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    List<Object> services = new ArrayList<>(serviceProviders.size());
    map.put("services", services);
    for (ServiceProvider serviceProvider : serviceProviders) {
      try {
        MappedStateCollector dump = new MappedStateCollector(serviceProvider.getClass().getName());
        serviceProvider.addStateTo(dump.subStateDumpCollector(serviceProvider.getClass().getName()));
        services.add(dump.getMap());
      } catch (Throwable t) {
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        t.printStackTrace(p);
        services.add(w.toString());
      }
    }

    for (ImplementationProvidedServiceProvider implementationProvidedServiceProvider : implementationProvidedServiceProviders) {
      try {
        MappedStateCollector dump = new MappedStateCollector(implementationProvidedServiceProvider.getClass().getName());
        implementationProvidedServiceProvider.addStateTo(dump);
        services.add(dump.getMap());
      } catch (Throwable t) {
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        t.printStackTrace(p);
        services.add(w.toString());
      }
    }
    return map;
  }

}
