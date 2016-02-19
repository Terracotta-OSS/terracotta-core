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

import com.tc.object.EntityID;
import com.tc.test.TCTestCase;

import java.io.IOException;

import org.junit.Assert;
import org.terracotta.exception.EntityException;


public class EntityPersistorTest extends TCTestCase {
  private static final String TEMP_FILE = "temp_file";
  private FlatFilePersistentStorage persistentStorage;
  private EntityPersistor entityPersistor;

  @Override
  public void setUp() {
    try {
      this.persistentStorage = new FlatFilePersistentStorage(getTempFile(TEMP_FILE));
      this.persistentStorage.create();
    } catch (IOException e) {
      fail(e);
    }
    this.entityPersistor = new EntityPersistor(this.persistentStorage);
  }

  /**
   * Test that an empty persistor correctly returns as empty for all calls and can be cleared.
   */
  public void testQueryEmpty() throws EntityException {
    // This should be safe and do nothing.
    this.entityPersistor.clear();
    
    // We expect to get an empty collection.
    Assert.assertTrue(0 == this.entityPersistor.loadEntityData().size());
    
    // The entity shouldn't be found.
    EntityID id = new EntityID("class name", "entity name");
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
  }

  /**
   * Test that a basic create/destroy works and can then be queried.
   */
  public void testSimpleCreateDestroy() throws EntityException {
    EntityID id = new EntityID("class name", "entity name");
       
    // Query that it doesn't exist.
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
    
    // Create the entity.
    long version = 1;
    long consumerID = 1;
    byte[] configuration = new byte[0];
    this.entityPersistor.entityCreated(id, version, consumerID, configuration);
    
    // Query that it exists.
    Assert.assertTrue(this.entityPersistor.containsEntity(id));
    
    // Destroy the entity.
    this.entityPersistor.entityDeleted(id);
    
    // Query that it doesn't exist.
    Assert.assertFalse(this.entityPersistor.containsEntity(id));
  }
}
