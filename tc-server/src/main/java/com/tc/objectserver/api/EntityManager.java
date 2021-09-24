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
import com.tc.entity.VoltronEntityMessage;
import com.tc.exception.ServerException;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.objectserver.entity.ServerEntityFactory;
import com.tc.text.PrettyPrintable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface EntityManager extends MessageCodecSupplier, PrettyPrintable {

  /**
   * The entity manager normally starts in a "passive" state but will be notified that it should become active when the server becomes active.
   */
  List<VoltronEntityMessage> enterActiveState();

  /**
   * Creates an non-existent entity
   *
   * @param id id of the entity to create
   * @param version the version of the entity on the calling client
   * @param consumerID the unique consumerID this entity uses when interacting with services
   * @return an uninitialized ManagedEntity
   */
  ManagedEntity createEntity(EntityID id, long version, long consumerID) throws ServerException;
 
  /**
   * Once a ManagedEntity is destroyed it must be removed from the EntityManager manually. 
   * @param id the fetchid of the destroyed entity
   * @return true if the entity is removed
   */
  boolean removeDestroyed(FetchID id);

  /**
   * Get the stub for the specified entity
   *  
   * @param descriptor
   * @return ManagedEntity wrapper for the entity
   */
  Optional<ManagedEntity> getEntity(EntityDescriptor descriptor) throws ServerException;
  
  /**
   * Creates an entity instance from existing storage.This case is called during restart.  The reason why configuration is provided here is because there is no external request acting on the entity, passing
 that information in.  In the case of "createEntity", a create request is handled by the entity, right after it is
 created whereas this call is stand-alone and the entity is ready for use immediately.
   * 
   * @param entityID id of the entity to create
   * @param recordedVersion the version of the entity's implementation from before the restart
   * @param consumerID the unique consumerID this entity uses when interacting with services
   * @param canDelete if the entity can be deleted by the user
   * @param configuration The opaque configuration to use in the creation.
   * @throws com.tc.exception.ServerException
   */
  void loadExisting(EntityID entityID, long recordedVersion, long consumerID, boolean canDelete, byte[] configuration) throws ServerException;
  
  void resetReferences();

  boolean canDelete(EntityID entityID);

  /**
   * Gets a snapshot of the entity list, sorted by order in which they were initially instantiated, under lock.
   * 
   * runFirst is allowed to consume the entire sorted list, prior to running runEach on each element in the list.
   * 
   * @param runFirst Consumes the entire list before it is iterated.
   * @return The sorted list.
   */
  List<ManagedEntity> snapshot(Predicate<ManagedEntity> runFirst);
  
  Collection<ManagedEntity> getAll();
  /**
   * 
   * @return the classloader used to create all entities
   */
  ServerEntityFactory getEntityLoader();
}
