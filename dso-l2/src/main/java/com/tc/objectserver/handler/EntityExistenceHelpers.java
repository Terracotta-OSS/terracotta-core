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

import com.tc.net.ClientID;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.util.Assert;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;


/**
 * Contains helpers which implement the entity existence-related patterns common to both ProcessTransactionHandler and
 * ReplicatedTransactionHandler.
 */
public class EntityExistenceHelpers {
  public static boolean createEntityReturnWasCached(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, long consumerID, byte[] configuration, boolean canDelete) throws EntityException {
    boolean resultWasCached = false;
    // We can't have a null client, transaction, or oldest transaction when an entity is created - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    
    if (entityPersistor.wasEntityCreatedInJournal(clientID, transactionID)) {
      // We either threw or did succeed in the journal, so just carry on as though we created it.
      resultWasCached = true;
    } else {
      // There is no record of this, so give it a try.
      long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
      try {
        entityManager.createEntity(entityID, version, consumerID, canDelete);
        // Record the success.
        entityPersistor.entityCreated(clientID, transactionID, oldestTransactionOnClient, entityID, version, consumerID, canDelete, configuration);
      } catch (EntityException e) {
        // Record the failure and re-throw.
        entityPersistor.entityCreateFailed(clientID, transactionID, oldestTransactionOnClient, e);
        throw e;
      }
    }
    return resultWasCached;
  }

  public static boolean destroyEntityReturnWasCached(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID) throws EntityException {
    boolean resultWasCached = false;
    // We can't have a null client, transaction, or oldest transaction when an entity is destroyed - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    
    if (entityPersistor.wasEntityDestroyedInJournal(clientID, transactionID)) {
      // This either threw or is already destroyed in the journal, so carry on, as though we destroyed it.
      resultWasCached = true;
    } else {
      // There is no record of this, so give it a try.
      long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
      try {
        entityManager.destroyEntity(entityID);
        // Record the success.
        entityPersistor.entityDestroyed(clientID, transactionID, oldestTransactionOnClient, entityID);
      } catch (EntityException e) {
        // Record the failure and re-throw.
        entityPersistor.entityDestroyFailed(clientID, transactionID, oldestTransactionOnClient, e);
        throw e;
      }
    }
    return resultWasCached;
  }

  /**
   * Note that this is the one case which doesn't expose the details of whether it satisfied the request from the cached result in the journal or checked, for the first time.
   * This is because the caller doesn't expose the difference to the entity but we may want to change that, in the future.
   */
  public static boolean doesExist(EntityPersistor entityPersistor, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID) {
    // We can't have a null client, transaction, or oldest transaction when an entity is checked - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
    
    return entityPersistor.containsEntity(clientID, transactionID, oldestTransactionOnClient, entityID);
  }

  // This is similar to the other cases except that it returns the cached result, if there is one, instead of just "true".
  public static byte[] reconfigureEntityReturnCachedResult(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, byte[] configuration) throws EntityException {
    // We can't have a null client, transaction, or oldest transaction when an entity is created - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
    
    byte[] cachedResult = entityPersistor.reconfiguredResultInJournal(clientID, transactionID);
    if (null != cachedResult) {
      // This means that this reconfigure was already completed and we can just return this cached result so that the caller doesn't notify the entity that it needs to do anything.
    } else {
      // There is no recorded attempt to perform this reconfigure so we need to do it, here.
      Optional<ManagedEntity> optionalEntity = entityManager.getEntity(entityID, version);
      if (optionalEntity.isPresent()) {
        entityPersistor.entityReconfigureSucceeded(clientID, transactionID, oldestTransactionOnClient, entityID, version, configuration);
      } else {
        EntityNotFoundException error = new EntityNotFoundException(entityID.getClassName(), entityID.getEntityName());
        entityPersistor.entityReconfigureFailed(clientID, transactionID, oldestTransactionOnClient, error);
      }
    }
    // Note that this will be null if this is the first time we did the reconfigure, meaning that the caller still needs to run the reconfigure on the entity.
    return cachedResult;
  }
}
