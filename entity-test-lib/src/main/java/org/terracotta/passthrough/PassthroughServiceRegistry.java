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

import com.google.common.collect.ImmutableMap;


/**
 * The registry of services available on a PassthroughServer.
 */
public class PassthroughServiceRegistry implements ServiceRegistry {
  private final long consumerID;
  private final Map<Class<?>, ServiceProvider> serviceProviderMap;
  
  public PassthroughServiceRegistry(long consumerID, List<ServiceProvider> serviceProviders) {
    this.consumerID = consumerID;
    
    Map<Class<?>, ServiceProvider> temp = new HashMap<Class<?>, ServiceProvider>();
    for(ServiceProvider provider : serviceProviders) {
      for (Class<?> serviceType : provider.getProvidedServiceTypes()) {
        // We currently have no way of handling multiple providers.
        Assert.assertTrue(null == temp.get(serviceType));
        temp.put(serviceType, provider);
      }
    }
    this.serviceProviderMap = ImmutableMap.copyOf(temp);
  }

  @Override
  public <T> T getService(ServiceConfiguration<T> configuration) {
    ServiceProvider provider = this.serviceProviderMap.get(configuration.getServiceType());
    T service = null;
    if (null != provider) {
      service = provider.getService(this.consumerID, configuration);
    }
    return service;
  }
}
