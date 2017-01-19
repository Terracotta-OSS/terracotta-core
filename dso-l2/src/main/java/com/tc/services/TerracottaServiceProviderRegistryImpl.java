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
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;


public class TerracottaServiceProviderRegistryImpl implements TerracottaServiceProviderRegistry {
  private static final TCLogger logger = TCLogging.getLogger(TerracottaServiceProviderRegistryImpl.class);

  // We need to hold on to the configuration when we are initialized so we can give it to services registered later as
  //  built-ins.
  private final Set<ServiceProvider> serviceProviders = new LinkedHashSet<>();
  private final Set<ImplementationProvidedServiceProvider> implementationProvidedServiceProviders = new LinkedHashSet<>();

  // In order to prevent ordering errors during start-up, we will set a flag when we hand out sub-registries and make sure that we haven't done that when registering a service.
  private boolean hasCreatedSubRegistries;

  @Override
  public void initialize(PlatformConfiguration platformConfiguration, TcConfiguration configuration, ClassLoader loader) {
    Assert.assertFalse(this.hasCreatedSubRegistries);
    final Map<Class<? extends ServiceProvider>, ServiceProviderTuple> serviceProviderTupleMap = findServiceProviders(configuration, loader);
    serviceProviderTupleMap.values().forEach((tuple) -> {
      tuple.getServiceProvider().initialize(tuple.getServiceProviderConfiguration(), platformConfiguration);
      registerNewServiceProvider(tuple.getServiceProvider());
    });
  }


  public Map<Class<? extends ServiceProvider>, ServiceProviderTuple> findServiceProviders(TcConfiguration tcConfiguration, ClassLoader loader) {
    final Map<Class<? extends ServiceProvider>, ServiceProviderTuple> serviceProviderTupleMap = new HashMap<>();
    List<ServiceProviderConfiguration> serviceProviderConfigurationList = tcConfiguration.getServiceConfigurations();
    if(serviceProviderConfigurationList != null) {
      for (ServiceProviderConfiguration config : serviceProviderConfigurationList) {
        Class<? extends ServiceProvider> serviceClazz = config.getServiceProviderType();
        try {
          ServiceProvider provider = serviceClazz.newInstance();
          serviceProviderTupleMap.put(config.getServiceProviderType(), new ServiceProviderTuple(provider, config));
        } catch (InstantiationException | IllegalAccessException ie) {
          logger.error("caught exception while initializing service " + serviceClazz, ie);
          throw new RuntimeException(ie);
        }
      }
    }
    loadClasspathBuiltins(serviceProviderTupleMap, loader);
    return serviceProviderTupleMap;
  }
  
  private void loadClasspathBuiltins(Map<Class<? extends ServiceProvider>, ServiceProviderTuple> serviceProviderTupleMap, ClassLoader loader) {
    List<Class<? extends ServiceProvider>> providers = ServiceLocator.getImplementations(ServiceProvider.class, loader);
    for (Class<? extends ServiceProvider> clazz : providers) {
      try {
        if (!clazz.isAnnotationPresent(BuiltinService.class)) {
          logger.warn("service:" + clazz.getName() + " is registered as a builtin but is not properly annotated with @BuiltinService.  This builtin will not be loaded");
        } else {
      // only add a builtin if one has not already been configured into the system via xml
          if (serviceProviderTupleMap.get(clazz) == null) {
            ServiceProvider serviceProvider = clazz.newInstance();
      //  there is no config for builtins
            serviceProviderTupleMap.put(clazz, new ServiceProviderTuple(serviceProvider, null));
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
    this.hasCreatedSubRegistries = true;
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

  /**
   * @return True if there is a user-provided service for the given class registered.
   */
  public boolean hasUserProvidedServiceProvider(TcConfiguration tcConfiguration, ClassLoader loader, Class<?> serviceInterface) {
    boolean hasProvider = false;
    Map<Class<? extends ServiceProvider>, ServiceProviderTuple> serviceProviderTupleMap = findServiceProviders(tcConfiguration, loader);
    for (ServiceProviderTuple serviceProviderTuple : serviceProviderTupleMap.values()) {
      if (serviceProviderTuple.getServiceProvider().getProvidedServiceTypes().contains(serviceInterface)) {
        hasProvider = true;
        break;
      }
    }
    return hasProvider;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    ToStringStateDumper dump = new ToStringStateDumper("services");
    dumpStateTo(dump);
    out.println(dump);
    return out;
  }

  private static class ServiceProviderTuple {
    private final ServiceProvider serviceProvider;
    private final ServiceProviderConfiguration serviceProviderConfiguration;

    private ServiceProviderTuple(ServiceProvider serviceProvider,
                                 ServiceProviderConfiguration serviceProviderConfiguration) {
      this.serviceProvider = serviceProvider;
      this.serviceProviderConfiguration = serviceProviderConfiguration;
    }

    public ServiceProvider getServiceProvider() {
      return serviceProvider;
    }

    public ServiceProviderConfiguration getServiceProviderConfiguration() {
      return serviceProviderConfiguration;
    }
  }
}
