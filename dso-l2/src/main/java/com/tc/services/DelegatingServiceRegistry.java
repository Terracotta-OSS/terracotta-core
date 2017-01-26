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

import com.tc.objectserver.api.ManagedEntity;
import com.tc.util.Assert;
import java.util.Collections;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DelegatingServiceRegistry implements InternalServiceRegistry {
  private final long consumerID;
  private final Map<Class<?>, List<ServiceProvider>> serviceProviderMap;
  private final Map<Class<?>, List<ImplementationProvidedServiceProvider>> implementationProvidedServiceProviderMap;
  // Both the registry and the entity refer to each other so this is late-bound.
  private ManagedEntity owningEntity;

  public DelegatingServiceRegistry(long consumerID, ServiceProvider[] providers, ImplementationProvidedServiceProvider[] implementationProvidedProviders) {
    this.consumerID = consumerID;
    
    Map<Class<?>, List<ServiceProvider>> tempProviders = new HashMap<>();
    for(ServiceProvider provider : providers) {
      for (Class<?> serviceType : provider.getProvidedServiceTypes()) {
        List<ServiceProvider> listForType = tempProviders.get(serviceType);
        if (null == listForType) {
          listForType = new LinkedList<>();
          tempProviders.put(serviceType, listForType);
        }
        listForType.add(provider);
      }
    }
    serviceProviderMap = Collections.unmodifiableMap(tempProviders);
    
    Map<Class<?>, List<ImplementationProvidedServiceProvider>> tempBuiltInProviders = new HashMap<>();
    for(ImplementationProvidedServiceProvider provider : implementationProvidedProviders) {
      for (Class<?> serviceType : provider.getProvidedServiceTypes()) {
        List<ImplementationProvidedServiceProvider> listForType = tempBuiltInProviders.get(serviceType);
        if (null == listForType) {
          listForType = new LinkedList<>();
          tempBuiltInProviders.put(serviceType, listForType);
        }
        listForType.add(provider);
      }
    }
    implementationProvidedServiceProviderMap = Collections.unmodifiableMap(tempBuiltInProviders);
  }

  @Override
  public <T> T getService(ServiceConfiguration<T> configuration) {
    T builtInService = getBuiltInService(configuration);
    T externalService = getExternalService(configuration);
    // Service look-ups cannot be ambiguous:  there must be precisely 0 or 1 successfully-returned service for a
    //  given type.
    if ((null != builtInService) && (null != externalService)) {
      throw new IllegalArgumentException("Both built-in and external service found for type: " + configuration.getServiceType());
    }
    return (null != builtInService)
        ? builtInService
        : externalService;
  }

  public void setOwningEntity(ManagedEntity entity) {
    Assert.assertNull(this.owningEntity);
    Assert.assertNotNull(entity);
    this.owningEntity = entity;
  }

  private <T> T getBuiltInService(ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();
    List<ImplementationProvidedServiceProvider> serviceProviders = implementationProvidedServiceProviderMap.get(serviceType);
    T service = null;
    if (null != serviceProviders) {
      for (ImplementationProvidedServiceProvider provider : serviceProviders) {
        T oneService = provider.getService(this.consumerID, this.owningEntity, configuration);
        if (null != oneService) {
          // Service look-ups cannot be ambiguous:  there must be precisely 0 or 1 successfully-returned service for a
          //  given type.
          if (null != service) {
            throw new IllegalArgumentException("Multiple built-in service providers matched for type: " + serviceType);
          } else {
            service = oneService;
          }
        }
      }
    }
    return service;
  }

  private <T> T getExternalService(ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();
    List<ServiceProvider> serviceProviders = serviceProviderMap.get(serviceType);
    if (serviceProviders == null) {
     return null;
    }
    T service = null;
    for (ServiceProvider provider : serviceProviders) {
      T oneService = provider.getService(this.consumerID, configuration);
      if (null != oneService) {
        // Service look-ups cannot be ambiguous:  there must be precisely 0 or 1 successfully-returned service for a
        //  given type.
        if (null != service) {
          throw new IllegalArgumentException("Multiple service providers matched for type: " + serviceType);
        } else {
          service = oneService;
        }
      }
    }
    return service;
  }
}
