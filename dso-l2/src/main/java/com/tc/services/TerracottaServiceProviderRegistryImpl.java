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

import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import org.terracotta.entity.ServiceConfiguration;


public class TerracottaServiceProviderRegistryImpl implements TerracottaServiceProviderRegistry {
  private static final TCLogger logger = TCLogging.getLogger(TerracottaServiceProviderRegistryImpl.class);

  // We need to hold on to the configuration when we are initialized so we can give it to services registered later as
  //  built-ins.
  private final Set<ServiceProvider> serviceProviders = new HashSet<>();
  private final Set<BuiltInServiceProvider> builtInServiceProviders = new HashSet<>();

  @Override
  public void initialize(String serverName, TcConfiguration configuration) {
    List<ServiceProviderConfiguration> serviceProviderConfigurationList = configuration.getServiceConfigurations().get(serverName);
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

  private void registerNewServiceProvider(ServiceProvider service) {
    logger.info("Initializing " + service);
    serviceProviders.add(service);
  }
}
