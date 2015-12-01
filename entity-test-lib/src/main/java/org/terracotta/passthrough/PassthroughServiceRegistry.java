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

import java.util.Map;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceRegistry;


/**
 * The registry of services available on a PassthroughServer.
 */
public class PassthroughServiceRegistry implements ServiceRegistry {
  private final long consumerID;
  private final Map<Class<?>, ServiceProvider> serviceProviderMap;
  
  public PassthroughServiceRegistry(long consumerID, Map<Class<?>, ServiceProvider> serviceProviderMap) {
    this.consumerID = consumerID;
    this.serviceProviderMap = serviceProviderMap;
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
