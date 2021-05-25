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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;

import java.util.List;
import org.terracotta.entity.ServiceException;


public class DelegatingServiceRegistry implements InternalServiceRegistry {
  private final long consumerID;
  private final Collection<ServiceProvider> serviceProviders;
  private final Collection<ImplementationProvidedServiceProvider> implementationProvidedServiceProviders;
  // Both the registry and the entity refer to each other so this is late-bound.
  private ManagedEntity owningEntity;

  public DelegatingServiceRegistry(long consumerID, ServiceProvider[] providers, ImplementationProvidedServiceProvider[] implementationProvidedProviders) {
    this.consumerID = consumerID;
    
    serviceProviders = Collections.unmodifiableCollection(Arrays.asList(providers));
    
    implementationProvidedServiceProviders = Collections.unmodifiableCollection(Arrays.asList(implementationProvidedProviders));
  }

  @Override
  public <T> T getService(ServiceConfiguration<T> configuration) throws ServiceException {
    T builtInService = getBuiltInService(configuration);
    T externalService = getExternalService(configuration);
    // Service look-ups cannot be ambiguous:  there must be precisely 0 or 1 successfully-returned service for a
    //  given type.
    if ((null != builtInService) && (null != externalService)) {
      throw new ServiceException("Both built-in and external service found for type: " + configuration.getServiceType());
    }
    return (null != builtInService)
        ? builtInService
        : externalService;
  }
  
  @Override
  public <T> Collection<T> getServices(ServiceConfiguration<T> configuration) {
    return getExternalServices(configuration);
  }
  
  @Override
  public void setOwningEntity(ManagedEntity entity) {
    Assert.assertNull(this.owningEntity);
    Assert.assertNotNull(entity);
    this.owningEntity = entity;
  }

  private <T> T getBuiltInService(ServiceConfiguration<T> configuration) throws ServiceException {
    Class<T> serviceType = configuration.getServiceType();
    T service = null;
    for (ImplementationProvidedServiceProvider provider : implementationProvidedServiceProviders) {
      if (provider.getProvidedServiceTypes().contains(serviceType)) {
        T oneService = provider.getService(this.consumerID, this.owningEntity, configuration);
        if (null != oneService) {
          // Service look-ups cannot be ambiguous:  there must be precisely 0 or 1 successfully-returned service for a
          //  given type.
          if (null != service) {
            throw new ServiceException("Multiple built-in service providers matched for type: " + serviceType);
          } else {
            service = oneService;
          }
        }
      }
    }
    return service;
  }

  private <T> T getExternalService(ServiceConfiguration<T> configuration) throws ServiceException {
    Class<T> serviceType = configuration.getServiceType();
    T theService = null;
    for (ServiceProvider provider : serviceProviders) {
      if (provider.getProvidedServiceTypes().contains(serviceType)) {
        T oneService = provider.getService(this.consumerID, configuration);
        if (null != oneService) {
          if (theService != null) {
            throw new ServiceException("Multiple service providers matched for type: " + serviceType);
          } else {
            theService = oneService;
          }
        }
      }
    }
    return theService;
  }
  
  private <T> Collection<T> getExternalServices(ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();
    List<T> choices = new ArrayList<>();

    for (ServiceProvider provider : serviceProviders) {
      if (provider.getProvidedServiceTypes().contains(serviceType)) {
        T oneService = provider.getService(this.consumerID, configuration);
        if (oneService != null) {
          choices.add(oneService);
        }
      }
    }
    return choices;
  }  
}
