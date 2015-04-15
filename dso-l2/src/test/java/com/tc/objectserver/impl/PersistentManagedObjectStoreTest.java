/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.ManagedObjectPersistor;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Arrays;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersistentManagedObjectStoreTest extends TestCase {

  private ManagedObjectPersistor persistor;
  private PersistentManagedObjectStore objectStore;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    persistor = mock(ManagedObjectPersistor.class);
    objectStore = new PersistentManagedObjectStore(persistor);
  }

  public void testGetObjectByID() throws Exception {
    ObjectID objectID = new ObjectID(1);
    objectStore.getObjectByID(objectID);
    verify(persistor).loadObjectByID(objectID);
  }

  public void testContainsObject() throws Exception {
    ObjectID objectID = new ObjectID(1);
    objectStore.containsObject(objectID);
    verify(persistor).containsObject(objectID);
  }

  public void testRemoveObjectsByID() throws Exception {
    ObjectIDSet objectIDs = new BitSetObjectIDSet(Arrays.asList(new ObjectID(1), new ObjectID(2)));
    objectStore.removeAllObjectsByID(objectIDs);
    verify(persistor).deleteAllObjects(objectIDs);
  }
}
