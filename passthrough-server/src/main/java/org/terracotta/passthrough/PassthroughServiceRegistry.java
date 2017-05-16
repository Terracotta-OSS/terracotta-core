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
                                    List<ServiceProvider> overrideServiceProviders, List<PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders, DeferredEntityContainer container) {
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    this.consumerID = consumerID;
    //  override service providers come first
    List<ServiceProvider> services = new ArrayList<ServiceProvider>(overrideServiceProviders);
    //  regular service providers come next
    services.addAll(serviceProviders);
    
    this.serviceProviders = Collections.unmodifiableList(services);

    this.implementationProvidedServiceProviders = Collections.unmodifiableList(implementationProvidedServiceProviders);
    
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

  @Override
  public <T> Collection<T> getServices(ServiceConfiguration<T> configuration) {
    return getExternals(configuration);
  }

  private <T> T getBuiltIn(ServiceConfiguration<T> configuration) {
    Class<?> serviceType = configuration.getServiceType();
    T service = null;
    for (PassthroughImplementationProvidedServiceProvider provider : this.implementationProvidedServiceProviders) {
      if (provider.getProvidedServiceTypes().contains(serviceType)) {
        service = provider.getService(this.entityClassName, this.entityName, this.consumerID, this.owningEntityContainer, configuration);
        if (service != null) {
          return service;
        }
      }
    }
    return null;
  }

  private <T> T getExternal(ServiceConfiguration<T> configuration) {
    for (ServiceProvider provider : this.serviceProviders) {
      if (provider.getProvidedServiceTypes().contains(configuration.getServiceType())) {
        T service = provider.getService(this.consumerID, configuration);
        if (service != null) {
          return service;
        }
      }
    }
    return null;
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
