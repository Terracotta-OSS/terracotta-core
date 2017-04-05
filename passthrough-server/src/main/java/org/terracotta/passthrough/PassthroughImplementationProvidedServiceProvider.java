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

import java.util.Collection;

import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.ServiceConfiguration;


/**
 * The interface exposed by the purely-internal services.  That is, those which are not provided by users but are part
 * of the server implementation.
 */
public interface PassthroughImplementationProvidedServiceProvider {
  /**
   * Get an instance of service from the provider for the entity with the corresponding class, name, and consumerID.
   *
   * @param entityClassName The fully-qualified name of the class (can be null if the entity is synthetic)
   * @param entityName The name of the entity instance (can be null if the entity is synthetic)
   * @param consumerID The unique ID used to name-space the returned service
   * @param container The container which will eventually hold the entity instance (may be null if this is a non-entity consumer)
   * @param configuration Service configuration which is to be used
   * @return service instance
   */
  <T> T getService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration);

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
   * to the platform.
   * If the entity actually tries to use a built-in service, in its constructor, and that service needs direct access
   * to the entity instance, it must register for notifyOnEntitySet and cache the request until it can apply it when the
   * instance is known.
   */
  public static class DeferredEntityContainer {
    public MessageCodec<?, ?> codec;
    private CommonServerEntity<?, ?> entity;
    private EntityContainerListener listener;
    
    public void notifyOnEntitySet(EntityContainerListener listener) {
      // Note that our current implementation only handles one such listener (only used by PassthroughMessengerService).
      Assert.assertTrue(null == this.listener);
      this.listener = listener;
    }
    public CommonServerEntity<?, ?> getEntity() {
      return this.entity;
    }
    public void setEntity(CommonServerEntity<?, ?> entity) {
      Assert.assertTrue(null == this.entity);
      Assert.assertTrue(null != entity);
      this.entity = entity;
      if (null != this.listener) {
        this.listener.entitySetInContainer(this);
      }
    }
    public void clearEntity() {
      this.entity = null;
    }
  }


  public interface EntityContainerListener {
    public void entitySetInContainer(DeferredEntityContainer container);
  }
}
