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
package com.tc.services;

import java.util.Collection;

import org.terracotta.entity.ServiceConfiguration;

import com.tc.objectserver.api.ManagedEntity;
import org.terracotta.entity.ServiceProviderCleanupException;


/**
 * This service provider implementation differs from the one used by user-provided services and is used exclusively by those
 * services which are provided by the platform implementation, itself.
 * This interface is correspondingly richer since it is part of the platform and exposes these details to built-in services
 * which are also part of the platform implementation.
 * The public ServiceProvider implementation is much more restricted in that user-provided services are not allowed to know
 * as many detail of the platform internals.
 * 
 * This has no explicit initialization routine as it is expected that the implementation will be initialized with rich
 * context, inline, prior to being registered with the platform's provider registry.
 */
public interface BuiltInServiceProvider {
  /**
   * Get an instance of service from the provider.
   *
   * @param consumerID The unique ID used to name-space the returned service
   * @param owningEntity The concrete entity which will own the server instance (may be null)
   * @param configuration Service configuration which is to be used
   * @return service instance
   */
  <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration);

  /**
   * Since a service provider can know how to build more than one type of service, this method allows the platform to query
   * the extent of that capability.  Returned is a collection of service types which can be returned by getService.
   *
   * @return A collection of the types of services which can be returned by the receiver.
   */
  Collection<Class<?>> getProvidedServiceTypes();

  /**
   * Clears up state for this ServiceProvider including any persisted state
   *
   * Generally platform calls this method during platform initialization so there won't be any entities using
   * underlying services
   *
   * If there are any failures when clearing state, this method should inform Platform by throwing {@link ServiceProviderCleanupException}
   *
   * @throws ServiceProviderCleanupException if there are any failures
   */
  void clear() throws ServiceProviderCleanupException;
}
