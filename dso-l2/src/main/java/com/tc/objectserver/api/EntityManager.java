package com.tc.objectserver.api;

import com.tc.object.EntityID;

import java.util.Optional;

import org.terracotta.exception.EntityException;

public interface EntityManager {

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
  void createEntity(EntityID id, long version, long consumerID) throws EntityException;

  /**
   * Deletes an existing entity.
   *
   * @param id id of the entity to delete
   */
  void destroyEntity(EntityID id) throws EntityException;

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
  void loadExisting(EntityID entityID, long recordedVersion, long consumerID, byte[] configuration) throws EntityException;

}
