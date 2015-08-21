package com.tc.services;

import org.terracotta.entity.Service;

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
  Service<T> entitySubservice(EntityID entityID);
}
