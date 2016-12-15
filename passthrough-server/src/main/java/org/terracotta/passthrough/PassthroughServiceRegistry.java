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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;

import com.google.common.collect.ImmutableMap;


/**
 * The registry of services available on a PassthroughServer.
 */
public class PassthroughServiceRegistry implements ServiceRegistry {
  private final String entityClassName;
  private final String entityName;
  private final long consumerID;
  private final Map<Class<?>, ServiceProvider> serviceProviderMap;
  private final Map<Class<?>, PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders;
  private final DeferredEntityContainer owningEntityContainer;
  
  public PassthroughServiceRegistry(String entityClassName, String entityName, long consumerID, List<ServiceProvider> serviceProviders,
                                    List<ServiceProvider> overrideServiceProviders, List<PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders, DeferredEntityContainer container) {
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    this.consumerID = consumerID;
    
    Map<Class<?>, ServiceProvider> overrideServiceTypes = new HashMap<Class<?>, ServiceProvider>();
    for (ServiceProvider overrideServiceProvider : overrideServiceProviders) {
      for (Class<?> serviceType : overrideServiceProvider.getProvidedServiceTypes()) {
        overrideServiceTypes.put(serviceType, overrideServiceProvider);
      }
    }

    Map<Class<?>, ServiceProvider> tempProviders = new HashMap<Class<?>, ServiceProvider>();
    for(ServiceProvider provider : serviceProviders) {
      for (Class<?> serviceType : provider.getProvidedServiceTypes()) {
        // We currently have no way of handling multiple providers.
        //Note that all override ServiceProviders are part of this serviceProviders 
        if(overrideServiceTypes.containsKey(serviceType)) {
          //install override ServiceProvider
          if(!tempProviders.containsKey(serviceType)) {
            tempProviders.put(serviceType, overrideServiceTypes.get(serviceType));
          }
        } else {
          //real server behaviour
          Assert.assertTrue(null == tempProviders.get(serviceType));
          tempProviders.put(serviceType, provider);
        }
      }
    }
    this.serviceProviderMap = ImmutableMap.copyOf(tempProviders);
    
    Map<Class<?>, PassthroughImplementationProvidedServiceProvider> tempInternalProviders = new HashMap<Class<?>, PassthroughImplementationProvidedServiceProvider>();
    for(PassthroughImplementationProvidedServiceProvider provider : implementationProvidedServiceProviders) {
      for (Class<?> serviceType : provider.getProvidedServiceTypes()) {
        // We currently have no way of handling multiple providers.
        Assert.assertTrue(null == tempInternalProviders.get(serviceType));
        tempInternalProviders.put(serviceType, provider);
      }
    }
    this.implementationProvidedServiceProviders = ImmutableMap.copyOf(tempInternalProviders);
    
    this.owningEntityContainer = container;
  }

  @Override
  public <T> T getService(ServiceConfiguration<T> configuration) {
    T builtInService = getBuiltIn(configuration);
    T externalService = getExternal(configuration);
    // We need at most one match.
    Assert.assertTrue((null == builtInService) || (null == externalService));
    return (null != builtInService)
        ? builtInService
        : externalService;
  }

  private <T> T getBuiltIn(ServiceConfiguration<T> configuration) {
    PassthroughImplementationProvidedServiceProvider provider = this.implementationProvidedServiceProviders.get(configuration.getServiceType());
    T service = null;
    if (null != provider) {
      service = provider.getService(this.entityClassName, this.entityName, this.consumerID, this.owningEntityContainer, configuration);
    }
    return service;
  }

  private <T> T getExternal(ServiceConfiguration<T> configuration) {
    ServiceProvider provider = this.serviceProviderMap.get(configuration.getServiceType());
    T service = null;
    if (null != provider) {
      service = provider.getService(this.consumerID, configuration);
    }
    return service;
  }

  public long getConsumerID() {
    return consumerID;
  }
}
