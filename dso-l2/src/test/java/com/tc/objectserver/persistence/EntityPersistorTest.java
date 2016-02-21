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
import com.tc.test.TCTestCase;

import java.io.IOException;

import org.junit.Assert;
import org.terracotta.exception.EntityException;

import static org.mockito.Mockito.mock;


public class EntityPersistorTest extends TCTestCase {
  private static final String TEMP_FILE = "temp_file";
  private FlatFilePersistentStorage persistentStorage;
  private EntityPersistor entityPersistor;
  private ClientID client;

  @Override
  public void setUp() {
    try {
      this.persistentStorage = new FlatFilePersistentStorage(getTempFile(TEMP_FILE));
      this.persistentStorage.create();
    } catch (IOException e) {
      fail(e);
    }
    this.entityPersistor = new EntityPersistor(this.persistentStorage);
    this.client = new ClientID(1);
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
    long doesExistTransactionID = 4;
    long oldestTransactionOnClient = 2;
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExistTransactionID, oldestTransactionOnClient, id));
    
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
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExistTransactionID, oldestTransactionOnClient, id));
    
    // Create the entity.
    long createTransactionID = 3;
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    this.entityPersistor.entityCreated(this.client, createTransactionID, oldestTransactionOnClient, id, version, consumerID, configuration);
    
    // Query that it exists.
    long doesExist2TransactionID = 4;
    Assert.assertTrue(this.entityPersistor.containsEntity(this.client, doesExist2TransactionID, oldestTransactionOnClient, id));
    
    // Destroy the entity.
    long destroyTransactionID = 5;
    this.entityPersistor.entityDestroyed(this.client, destroyTransactionID, oldestTransactionOnClient, id);
    
    // Query that it doesn't exist.
    long doesExist3TransactionID = 6;
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExist3TransactionID, oldestTransactionOnClient, id));
  }

  /**
   * Test that a basic create, reconfigure, destroy sequence works.
   */
  public void testSimpleCreateReconfigureDestroy() throws EntityException {
    EntityID id = new EntityID("class name", "entity name");
    long oldestTransactionOnClient = 1;
       
    // Query that it doesn't exist.
    long doesExistTransactionID = 2;
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExistTransactionID, oldestTransactionOnClient, id));
    
    // Create the entity.
    long createTransactionID = 3;
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    this.entityPersistor.entityCreated(this.client, createTransactionID, oldestTransactionOnClient, id, version, consumerID, configuration);
    
    // Reconfigure.
    long reconfigureTransactionID = 4;
    byte[] newConfiguration = new byte[1];
    this.entityPersistor.entityReconfigureSucceeded(this.client, reconfigureTransactionID, oldestTransactionOnClient, id, version, newConfiguration);
    
    // Destroy the entity.
    long destroyTransactionID = 5;
    this.entityPersistor.entityDestroyed(this.client, destroyTransactionID, oldestTransactionOnClient, id);
    
    // Query that it doesn't exist.
    long doesExist3TransactionID = 6;
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExist3TransactionID, oldestTransactionOnClient, id));
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
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExist1, oldestTransactionOnClient, id));
    long create1 = 2;
    this.entityPersistor.entityCreated(this.client, create1, oldestTransactionOnClient, id, version, consumerID, configuration);
    long doesExist2 = 3;
    Assert.assertTrue(this.entityPersistor.containsEntity(this.client, doesExist2, oldestTransactionOnClient, id));
    long destroy1 = 4;
    this.entityPersistor.entityDestroyed(this.client, destroy1, oldestTransactionOnClient, id);
    long doesExist3 = 5;
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExist3, oldestTransactionOnClient, id));
    long create2 = 6;
    this.entityPersistor.entityCreated(this.client, create2, oldestTransactionOnClient, id, version, consumerID, configuration);
    long doesExist4 = 7;
    Assert.assertTrue(this.entityPersistor.containsEntity(this.client, doesExist4, oldestTransactionOnClient, id));
    long destroy2 = 8;
    this.entityPersistor.entityDestroyed(this.client, destroy2, oldestTransactionOnClient, id);
    long doesExist5 = 9;
    Assert.assertFalse(this.entityPersistor.containsEntity(this.client, doesExist5, oldestTransactionOnClient, id));
    
    // Observe that re-issuing these commands yields the same results, even if they would be interpreted differently, if not part of the journal.
    Assert.assertTrue(this.entityPersistor.wasEntityDestroyedInJournal(this.client, destroy2));
    Assert.assertTrue(this.entityPersistor.wasEntityDestroyedInJournal(this.client, destroy1));
    Assert.assertTrue(this.entityPersistor.containsEntity(this.client, doesExist2, oldestTransactionOnClient, id));
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
    this.entityPersistor.entityCreated(this.client, create1, oldestTransactionOnClient, id, version, consumerID, configuration);
    long destroy1 = 2;
    this.entityPersistor.entityDestroyed(this.client, destroy1, oldestTransactionOnClient, id);
    oldestTransactionOnClient = 2;
    long create2 = 3;
    this.entityPersistor.entityCreated(this.client, create2, oldestTransactionOnClient, id, version, consumerID, configuration);
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
    this.entityPersistor.entityCreated(this.client, create1, oldestTransactionOnClient, id, version, consumerID, configuration);
    // Synthesize a failure (double-create).
    long create2 = 2;
    EntityException error = mock(EntityException.class);
    this.entityPersistor.entityCreateFailed(this.client, create2, oldestTransactionOnClient, error);
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
}
