/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.ManagedObjectPersistor;
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
    ObjectIDSet objectIDs = new ObjectIDSet(Arrays.asList(new ObjectID(1), new ObjectID(2)));
    objectStore.removeAllObjectsByID(objectIDs);
    verify(persistor).deleteAllObjects(objectIDs);
  }
}
