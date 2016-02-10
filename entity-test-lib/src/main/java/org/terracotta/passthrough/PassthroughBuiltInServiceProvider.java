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

import java.io.Closeable;
import java.util.Collection;

import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.ServiceConfiguration;


public interface PassthroughBuiltInServiceProvider extends Closeable {
  /**
   * Get an instance of service from the provider.
   *
   * @param consumerID The unique ID used to name-space the returned service
   * @param container The container which will eventually hold the entity instance (may be null if this is a non-entity consumer)
   * @param configuration Service configuration which is to be used
   * @return service instance
   */
  <T> T getService(long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration);

  /**
   * Since a service provider can know how to build more than one type of service, this method allows the platform to query
   * the extent of that capability.  Returned is a collection of service types which can be returned by getService.
   *
   * @return A collection of the types of services which can be returned by the receiver.
   */
  Collection<Class<?>> getProvidedServiceTypes();


  /**
   * We currently don't have any abstract entity container which is created before the entity is physically instantiated
   * so we use this class in order to describe where an entity will eventually live when creating the service instance for
   * this entity.
   * The reason for this is that the entity constructor may ask to get a service which means that the entity isn't yet known
   * to the platform.  So long as the entity doesn't try to use a built-in service in its constructor, but only requests it,
   * then this allows us to defer the connection.
   */
  public static class DeferredEntityContainer {
    public CommonServerEntity<?, ?> entity;
  }
}
