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
package com.tc.objectserver.persistence;

import com.tc.net.ClientID;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.test.TCTestCase;
import java.util.Collections;

import org.junit.Assert;
import org.terracotta.exception.EntityException;

import org.terracotta.exception.EntityNotFoundException;


public class EntityPersistorTest extends TCTestCase {
  private NullPlatformPersistentStorage persistentStorage;
  private EntityPersistor entityPersistor;
  private ClientID client;

  @Override
  public void setUp() {
    this.persistentStorage = new NullPlatformPersistentStorage();
    this.entityPersistor = new EntityPersistor(this.persistentStorage);
    this.client = new ClientID(1);
    this.entityPersistor.addTrackingForClient(client);
  }

  /**
   * Test that an empty persistor correctly returns as empty for all calls and can be cleared.
   */
  public void testQueryEmpty() throws EntityException {
    // This should be safe and do nothing.
    this.entityPersistor.clear();
    
    // We expect to get an empty collection.
    Assert.assertTrue(0 == this.entityPersistor.loadEntityData().size());
    
    // The entity shouldn't be found (note that this call will write an entry to the journal).
    EntityID id = new EntityID("class name", "entity name");
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
    
    // Journal queries should also be safe, when empty.
    long createdTransactionID = 5;
    Assert.assertFalse(this.entityPersistor.wasEntityCreatedInJournal(this.client, createdTransactionID));
    long destroyedTransactionID = 6;
    Assert.assertFalse(this.entityPersistor.wasEntityDestroyedInJournal(this.client, destroyedTransactionID));
    
    // We should also be able to remove tracking for this client.
    this.entityPersistor.removeTrackingForClient(this.client);
  }

  /**
   * Test that a basic create/destroy works and can then be queried.
   */
  public void testSimpleCreateDestroy() throws EntityException {
    EntityID id = new EntityID("class name", "entity name");
    long oldestTransactionOnClient = 1;
    
    // Query that it doesn't exist.
    long doesExistTransactionID = 2;
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
    
    // Create the entity.
    long createTransactionID = 3;
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    this.entityPersistor.entityCreated(this.client, createTransactionID, oldestTransactionOnClient, id, version, consumerID, true, configuration);
    
    // Query that it exists.
    long doesExist2TransactionID = 4;
    Assert.assertTrue(this.entityPersistor.containsEntity(id));
    
    // Destroy the entity.
    long destroyTransactionID = 5;
    this.entityPersistor.entityDestroyed(this.client, destroyTransactionID, oldestTransactionOnClient, id);
    
    // Query that it doesn't exist.
    long doesExist3TransactionID = 6;
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
  }

  /**
   * Test that a basic create, reconfigure, destroy sequence works.
   */
  public void testSimpleCreateReconfigureDestroy() throws EntityException {
    EntityID id = new EntityID("class name", "entity name");
    long oldestTransactionOnClient = 1;
       
    // Query that it doesn't exist.
    long doesExistTransactionID = 2;
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
    
    // Create the entity.
    long createTransactionID = 3;
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    this.entityPersistor.entityCreated(this.client, createTransactionID, oldestTransactionOnClient, id, version, consumerID, true, configuration);
    
    // Reconfigure.
    long reconfigureTransactionID = 4;
    byte[] newConfiguration = new byte[1];
    this.entityPersistor.entityReconfigureSucceeded(this.client, reconfigureTransactionID, oldestTransactionOnClient, id, version, newConfiguration);
    
    // Destroy the entity.
    long destroyTransactionID = 5;
    this.entityPersistor.entityDestroyed(this.client, destroyTransactionID, oldestTransactionOnClient, id);
    
    // Query that it doesn't exist.
    long doesExist3TransactionID = 6;
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
  }

  /**
   * Test that a sequence of create/destroy operations is correctly written into the journal such that re-sends get the same answer.
   */
  public void testCreateDestroyResend() throws EntityException {
    EntityID id = new EntityID("class name", "entity name");
    long oldestTransactionOnClient = 1;
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    
    // Create and destroy the entity, twice, checking existence at various points.
    long doesExist1 = 1;
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
    long create1 = 2;
    this.entityPersistor.entityCreated(this.client, create1, oldestTransactionOnClient, id, version, consumerID, true, configuration);
    long doesExist2 = 3;
    Assert.assertTrue(this.entityPersistor.containsEntity(id));
    long destroy1 = 4;
    this.entityPersistor.entityDestroyed(this.client, destroy1, oldestTransactionOnClient, id);
    long doesExist3 = 5;
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
    long create2 = 6;
    this.entityPersistor.entityCreated(this.client, create2, oldestTransactionOnClient, id, version, consumerID, true, configuration);
    long doesExist4 = 7;
    Assert.assertTrue(this.entityPersistor.containsEntity(id));
    long destroy2 = 8;
    this.entityPersistor.entityDestroyed(this.client, destroy2, oldestTransactionOnClient, id);
    long doesExist5 = 9;
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
    
    // Observe that re-issuing these commands yields the same results, even if they would be interpreted differently, if not part of the journal.
    Assert.assertTrue(this.entityPersistor.wasEntityDestroyedInJournal(this.client, destroy2));
    Assert.assertTrue(this.entityPersistor.wasEntityDestroyedInJournal(this.client, destroy1));
  }

  /**
   * Test that old journal entries are cleared such that attempts to use retired transaction IDs fail.
   */
  public void testCreateDestroyClearJournal() throws EntityException {
    EntityID id = new EntityID("class name", "entity name");
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    
    // Create and destroy an entity, twice, but update the oldest transactions such that the journal will be kept tightly restricted.
    long oldestTransactionOnClient = 1;
    long create1 = 1;
    this.entityPersistor.entityCreated(this.client, create1, oldestTransactionOnClient, id, version, consumerID, true, configuration);
    long destroy1 = 2;
    this.entityPersistor.entityDestroyed(this.client, destroy1, oldestTransactionOnClient, id);
    oldestTransactionOnClient = 2;
    long create2 = 3;
    this.entityPersistor.entityCreated(this.client, create2, oldestTransactionOnClient, id, version, consumerID, true, configuration);
    oldestTransactionOnClient = 3;
    long destroy2 = 4;
    this.entityPersistor.entityDestroyed(this.client, destroy2, oldestTransactionOnClient, id);
    
    // Verify that the older items are not known.
    Assert.assertFalse(this.entityPersistor.wasEntityCreatedInJournal(this.client, create1));
    Assert.assertFalse(this.entityPersistor.wasEntityDestroyedInJournal(this.client, destroy1));
  }

  /**
   * Test that a failed create still fails on re-send, even if it shouldn't fail if re-run as a fresh transaction.
   */
  public void testResentCreateFail() throws EntityException {
    EntityID id = new EntityID("class name", "entity name");
    long oldestTransactionOnClient = 1;
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    
    // Create an entity.
    long create1 = 1;
    this.entityPersistor.entityCreated(this.client, create1, oldestTransactionOnClient, id, version, consumerID, true, configuration);
    // Synthesize a failure (double-create).
    long create2 = 2;
    EntityException error = new EntityNotFoundException("class", "name");
    this.entityPersistor.entityCreateFailed(id, this.client, create2, oldestTransactionOnClient, error);
    // Destroy the entity.
    long destroy1 = 3;
    this.entityPersistor.entityDestroyed(this.client, destroy1, oldestTransactionOnClient, id);
    
    // Simulate a re-send of the failing create, observing that it still fails, even though the entity has been destroyed.
    try {
      this.entityPersistor.wasEntityCreatedInJournal(this.client, create2);
      // This should have thrown exception.
      fail();
    } catch (EntityException e) {
      // Expected.
    }
  }
  
  public void testOrphanedClientGC() throws Exception {
    this.entityPersistor.entityCreated(client, 1L, 0L, new EntityID("test", "test"), 1L, 1L, true, new byte[0]);
    this.entityPersistor.removeOrphanedClientsFromJournal(Collections.emptySet());
    Assert.assertFalse(this.entityPersistor.wasEntityCreatedInJournal(client, 1L));
  }
  
  public void testPermanentEntityCreation() throws Exception {
    EntityID eid = new EntityID("test", "test");
    this.entityPersistor.entityCreatedNoJournal(eid, 1L, 1L, false, new byte[0]);
    this.entityPersistor.waitForPermanentEntityCreation(eid);
    Assert.assertTrue(this.entityPersistor.containsEntity(eid));
  }  
  
  
  public void testPermanentEntityCreationFailed() throws Exception {
    EntityID eid = new EntityID("test", "test");
    this.entityPersistor.entityCreateFailed(eid, ClientID.NULL_ID, TransactionID.NULL_ID.toLong(), TransactionID.NULL_ID.toLong(), new EntityException("test", "test", "", new RuntimeException()) {
    });
    try {
      this.entityPersistor.waitForPermanentEntityCreation(eid);
      Assert.fail();
    } catch (Exception e) {
      // expected
    }
  }    
}
