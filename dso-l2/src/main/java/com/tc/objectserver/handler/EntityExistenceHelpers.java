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
    // if the clientID is null, then the create was server generated, don't log it or check it
    if (!clientID.isNull() && entityPersistor.wasEntityCreatedInJournal(clientID, transactionID)) {
      // We either threw or did succeed in the journal, so just carry on as though we created it.
      resultWasCached = true;
    } else {
      // There is no record of this, so give it a try.
      entityManager.createEntity(entityID, version, consumerID, canDelete ? 0 : ManagedEntity.UNDELETABLE_ENTITY);
    }
    return resultWasCached;
  }
  
  public static void recordEntityCreate(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, long consumerID, byte[] configuration, boolean canDelete, EntityException exception) {
    long transactionID = transactionIDObject.toLong();
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
      // Record the success.
  // if the clientID is null, then the create was server generated, don't log it or check it
    if (!clientID.isNull()) {
      if (exception == null) {
        entityPersistor.entityCreated(clientID, transactionID, oldestTransactionOnClient, entityID, version, consumerID, canDelete, configuration);
      } else {
        entityPersistor.entityCreateFailed(clientID, transactionID, oldestTransactionOnClient, exception);
      }
    }
  }

  public static boolean destroyEntityReturnWasCached(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID) throws EntityException {
    // We can't have a null client, transaction, or oldest transaction when an entity is destroyed - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertFalse(clientID.isNull());
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
      
    return entityPersistor.wasEntityDestroyedInJournal(clientID, transactionID);
  }
  
  public static void recordDestroyEntity(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, EntityException exception) {
    // We can't have a null client, transaction, or oldest transaction when an entity is destroyed - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertFalse(clientID.isNull());
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
      // There is no record of this, so give it a try.
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
    
      // Record the success.
    if (exception == null) {
      entityPersistor.entityDestroyed(clientID, transactionID, oldestTransactionOnClient, entityID);
    } else {
      entityPersistor.entityDestroyFailed(clientID, transactionID, oldestTransactionOnClient, exception);
    }
  }  

  /**
   * Note that this is the one case which doesn't expose the details of whether it satisfied the request from the cached result in the journal or checked, for the first time.
   * This is because the caller doesn't expose the difference to the entity but we may want to change that, in the future.
   */
  public static boolean doesExist(EntityPersistor entityPersistor, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID) {
    // We can't have a null client, transaction, or oldest transaction when an entity is checked - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertFalse(clientID.isNull());
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
    
    return entityPersistor.containsEntity(clientID, transactionID, oldestTransactionOnClient, entityID);
  }
  
  public static void recordReconfigureEntity(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, byte[] configuration, EntityException exception) {
    // We can't have a null client, transaction, or oldest transaction when an entity is created - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertFalse(clientID.isNull());
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();

    if (exception == null) {
      entityPersistor.entityReconfigureSucceeded(clientID, transactionID, oldestTransactionOnClient, entityID, version, configuration);
    } else {
      entityPersistor.entityReconfigureFailed(clientID, transactionID, oldestTransactionOnClient, exception);
    }
  }
  // This is similar to the other cases except that it returns the cached result, if there is one, instead of just "true".
  public static byte[] reconfigureEntityReturnCachedResult(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, byte[] configuration) throws EntityException {
    // We can't have a null client, transaction, or oldest transaction when an entity is created - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertFalse(clientID.isNull());
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();
    
    return entityPersistor.reconfiguredResultInJournal(clientID, transactionID);
  }
}
