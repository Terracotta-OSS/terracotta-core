/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
