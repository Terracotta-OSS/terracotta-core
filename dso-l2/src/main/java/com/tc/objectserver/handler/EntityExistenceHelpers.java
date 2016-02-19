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
package com.tc.objectserver.handler;

import java.util.Optional;

import com.tc.object.EntityID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.persistence.EntityPersistor;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;


/**
 * Contains helpers which implement the entity existence-related patterns common to both ProcessTransactionHandler and
 * ReplicatedTransactionHandler.
 */
public class EntityExistenceHelpers {
  public static void createEntity(EntityPersistor entityPersistor, EntityManager entityManager, EntityID entityID, long version, long consumerID, byte[] configuration) throws EntityException {
    entityManager.createEntity(entityID, version, consumerID);
    entityPersistor.entityCreated(entityID, version, consumerID, configuration);
  }

  public static void destroyEntity(EntityPersistor entityPersistor, EntityManager entityManager, EntityID entityID) throws EntityException {
    entityManager.destroyEntity(entityID);
    entityPersistor.entityDeleted(entityID);
  }

  public static void reconfigureEntity(EntityPersistor entityPersistor, EntityManager entityManager, EntityID entityID, long version, byte[] configuration) throws EntityException {
    Optional<ManagedEntity> optionalEntity = entityManager.getEntity(entityID, version);
    if (optionalEntity.isPresent()) {
      entityPersistor.reconfigureEntity(entityID, version, configuration);
    } else {
      throw new EntityNotFoundException(entityID.getClassName(), entityID.getEntityName());
    }
  }
}
