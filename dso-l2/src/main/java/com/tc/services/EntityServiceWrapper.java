package com.tc.services;


import com.tc.object.EntityID;

/**
 * @author twu
 */
public interface EntityServiceWrapper<T> {
  /**
   * Grab the built-in subservice for an entity.
   *
   * @param entityID id of the entity
   * @return service
   */
  T entitySubservice(EntityID entityID);
}
