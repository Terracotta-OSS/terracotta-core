/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.objectserver.handler;

import com.tc.exception.ServerException;
import com.tc.net.ClientID;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.util.Assert;

/**
 * Contains helpers which implement the entity existence-related patterns common to both ProcessTransactionHandler and
 * ReplicatedTransactionHandler.
 */
public class EntityExistenceHelpers {
  public static boolean createEntityReturnWasCached(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, long consumerID, byte[] configuration) throws ServerException {
    boolean resultWasCached = false;
    // We can't have a null client, transaction, or oldest transaction when an entity is created - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    
    long transactionID = transactionIDObject.toLong();
    // if the clientID is null, then the create was server generated, don't log it or check it
    if (!clientID.isNull() && entityPersistor.wasEntityCreatedInJournal(entityID, clientID, transactionID)) {
      // We either threw or did succeed in the journal, so just carry on as though we created it.
      resultWasCached = true;
    } else {
      // There is no record of this, so give it a try.
      entityManager.createEntity(entityID, version, consumerID);
    }
    return resultWasCached;
  }

  public static boolean destroyEntityReturnWasCached(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID) throws ServerException {
    // We can't have a null client, transaction, or oldest transaction when an entity is destroyed - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertFalse(clientID.isNull());
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
      
    return entityPersistor.wasEntityDestroyedInJournal(entityID, clientID, transactionID);
  }
  
  public static void recordDestroyEntity(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, ServerException exception) {
    // We can't have a null client, transaction, or oldest transaction when an entity is destroyed - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
      // There is no record of this, so give it a try.
    long oldestTransactionOnClient = oldestTransactionOnClientObject.toLong();

    if (exception == null) {
      entityPersistor.entityDestroyed(clientID, transactionID, oldestTransactionOnClient, entityID);
    } else {
      entityPersistor.entityDestroyFailed(clientID, transactionID, oldestTransactionOnClient, exception);
    }
  }  
  
  public static void recordReconfigureEntity(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, byte[] configuration, ServerException exception) {
    // We can't have a null client, transaction, or oldest transaction when an entity is created - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
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
  public static byte[] reconfigureEntityReturnCachedResult(EntityPersistor entityPersistor, EntityManager entityManager, ClientID clientID, TransactionID transactionIDObject, TransactionID oldestTransactionOnClientObject, EntityID entityID, long version, byte[] configuration) throws ServerException {
    // We can't have a null client, transaction, or oldest transaction when an entity is created - even synthetic clients shouldn't do this as they will disrupt clients.
    Assert.assertNotNull(clientID);
    Assert.assertFalse(clientID.isNull());
    Assert.assertNotNull(transactionIDObject);
    Assert.assertNotNull(oldestTransactionOnClientObject);
    long transactionID = transactionIDObject.toLong();
    
    return entityPersistor.reconfiguredResultInJournal(entityID, clientID, transactionID);
  }
}
