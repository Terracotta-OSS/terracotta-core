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

package com.tc.objectserver.api;

import com.tc.entity.MessageCodecSupplier;
import com.tc.object.EntityID;
import java.util.Collection;

import java.util.Optional;
import java.util.function.Consumer;
import org.terracotta.entity.EntityMessage;

import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.exception.EntityException;

public interface EntityManager extends StateDumpable, MessageCodecSupplier {

  /**
   * The entity manager normally starts in a "passive" state but will be notified that it should become active when the server becomes active.
   */
  void enterActiveState();

  /**
   * Creates an non-existent entity
   *
   * @param id id of the entity to create
   * @param version the version of the entity on the calling client
   * @param consumerID the unique consumerID this entity uses when interacting with services
   */
  ManagedEntity createEntity(EntityID id, long version, long consumerID, int reference) throws EntityException;
 
  /**
   * Once a ManagedEntity is destroyed it must be removed from the EntityManager manually. 
   * @param id - EntityID
   * @return true if the entity is removed
   */
  boolean removeDestroyed(EntityID id);

  /**
   * Get the stub for the specified entity
   *  
   * @param id entity id
   * @param version the version of the entity on the calling client
   * @return ManagedEntity wrapper for the entity
   */
  Optional<ManagedEntity> getEntity(EntityID id, long version) throws EntityException;

  /**
   * Creates an entity instance from existing storage.  This case is called during restart.
   * 
   * The reason why configuration is provided here is because there is no external request acting on the entity, passing
   * that information in.  In the case of "createEntity", a create request is handled by the entity, right after it is
   * created whereas this call is stand-alone and the entity is ready for use immediately.
   * 
   * @param id id of the entity to create
   * @param recordedVersion the version of the entity's implementation from before the restart
   * @param consumerID the unique consumerID this entity uses when interacting with services
   * @param configuration The opaque configuration to use in the creation.
   */
  void loadExisting(EntityID entityID, long recordedVersion, long consumerID, boolean canDelete, byte[] configuration) throws EntityException;
  
  void resetReferences();

  /**
   * get the collection of managed entities under lock.  
   * @param runFirst
   * @param runEach
   * @param runLast
   * @return 
   */
  Collection<ManagedEntity> snapshot(Runnable runFirst, Consumer<ManagedEntity> runEach, Runnable runLast);
  
  Collection<ManagedEntity> getAll();
  /**
   * 
   * @return the classloader used to create all entities
   */
  ClassLoader getEntityLoader();
  
  SyncMessageCodec<EntityMessage> getSyncMessageCodec(EntityID eid);
}
