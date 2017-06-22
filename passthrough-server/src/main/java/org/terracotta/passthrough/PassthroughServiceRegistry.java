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

import java.util.List;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.terracotta.entity.ServiceException;


/**
 * The registry of services available on a PassthroughServer.
 */
public class PassthroughServiceRegistry implements ServiceRegistry {
  private final String entityClassName;
  private final String entityName;
  private final long consumerID;
  private final List<ServiceProvider> serviceProviders;
  private final List<PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders;
  private final DeferredEntityContainer owningEntityContainer;
  
  public PassthroughServiceRegistry(String entityClassName, String entityName, long consumerID, List<ServiceProvider> serviceProviders,
      List<PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders, DeferredEntityContainer container) {
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    this.consumerID = consumerID;

    List<ServiceProvider> services = new ArrayList<ServiceProvider>(serviceProviders);
    
    this.serviceProviders = Collections.unmodifiableList(services);

    this.implementationProvidedServiceProviders = Collections.unmodifiableList(implementationProvidedServiceProviders);
    
    this.owningEntityContainer = container;
  }

  @Override
  public <T> T getService(ServiceConfiguration<T> configuration) throws ServiceException {
    T builtInService = getBuiltIn(configuration);
    T externalService = getExternal(configuration);
    
    if (builtInService != null && externalService != null) {
      throw new ServiceException("multiple services defined");
    }
    // We need at most one match.
    Assert.assertTrue((null == builtInService) || (null == externalService));
    return (null != builtInService)
        ? builtInService
        : externalService;
  }

  @Override
  public <T> Collection<T> getServices(ServiceConfiguration<T> configuration) {
    return getExternals(configuration);
  }

  private <T> T getBuiltIn(ServiceConfiguration<T> configuration) throws ServiceException {
    Class<?> serviceType = configuration.getServiceType();
    T rService = null;
    for (PassthroughImplementationProvidedServiceProvider provider : this.implementationProvidedServiceProviders) {
      if (provider.getProvidedServiceTypes().contains(serviceType)) {
        T service = provider.getService(this.entityClassName, this.entityName, this.consumerID, this.owningEntityContainer, configuration);
        if (service != null) {
          if (rService != null) {
            throw new ServiceException("multiple services defined");
          } else {
            return service;
          }
        }
      }
    }
    return null;
  }

  private <T> T getExternal(ServiceConfiguration<T> configuration) throws ServiceException {
    T rService = null;
    for (ServiceProvider provider : this.serviceProviders) {
      if (provider.getProvidedServiceTypes().contains(configuration.getServiceType())) {
        T service = provider.getService(this.consumerID, configuration);
        if (service != null) {
          if (rService != null) {
            throw new ServiceException("multiple services defined");
          } else {
            rService = service;
          }
        }
      }
    }
    return rService;
  }

  private <T> Collection<T> getExternals(ServiceConfiguration<T> configuration) {
    List<T> items = new ArrayList<T>();
    for (ServiceProvider provider : this.serviceProviders) {
      if (provider.getProvidedServiceTypes().contains(configuration.getServiceType())) {
        T service = provider.getService(this.consumerID, configuration);
        if (service != null) {
          items.add(service);
        }
      }
    }
    return items;
  }
  
  public long getConsumerID() {
    return consumerID;
  }
}
