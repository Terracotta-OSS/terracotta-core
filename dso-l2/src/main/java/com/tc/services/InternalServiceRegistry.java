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
package com.tc.services;

import com.tc.objectserver.api.ManagedEntity;

import org.terracotta.entity.ServiceRegistry;


/**
 * An extension to ServiceRegistry used to expose late-binding of the entity which owns the service registry.
 * 
 * Note that not all registries are owned by ManagedEntity instances.
 */
public interface InternalServiceRegistry extends ServiceRegistry {
  /**
   * Sets the owner of the service registry in case it needs to know anything about or communicate with its owner.
   * Note that this method may never be called on an instance, if it has a non-entity owner.  If it is called, however, it
   * will not be null.
   * 
   * @param entity The owning entity (not null).
   */
  public void setOwningEntity(ManagedEntity entity);
}
