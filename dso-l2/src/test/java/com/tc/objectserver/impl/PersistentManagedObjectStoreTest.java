/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.impl.MockSink;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import junit.framework.TestCase;

public class PersistentManagedObjectStoreTest extends TestCase {

  private Map                          map;
  private TestManagedObjectPersistor   persistor;
  private PersistentManagedObjectStore objectStore;

  protected void setUp() throws Exception {
    super.setUp();
    map = new HashMap();
    persistor = new TestManagedObjectPersistor(map);
    objectStore = new PersistentManagedObjectStore(persistor, new MockSink());
  }

  public void testGetObjectByID() throws Exception {
    ObjectID objectID = new ObjectID(1);
    TestManagedObject mo = new TestManagedObject(objectID);
    persistor.map.put(objectID, mo);
    objectStore.getObjectByID(objectID);
    assertEquals(objectID, persistor.loadByObjectIDCalls.poll(0));
  }

  public void testAddRemovePutAndPutAll() {
    Collection managed = new ArrayList();
    TreeSet managedIDs = new TreeSet();
    for (int i = 0; i < 10; i++) {
      ObjectID id = new ObjectID(i);
      managedIDs.add(id);
      managed.add(new TestManagedObject(id));
    }
    assertEquals(0, map.size());
    for (Iterator i = managed.iterator(); i.hasNext();) {
      ManagedObject o = (ManagedObject) i.next();
      assertFalse(objectStore.containsObject(o.getID()));
      // add new shouldn't commit the object to the data store yet, but it should keep track of
      // the reference.
      objectStore.addNewObject(o);
      assertTrue(objectStore.containsObject(o.getID()));
      assertFalse(map.containsKey(o.getID()));

      // remove should remove the local reference.
      TreeSet toDelete = new TreeSet();
      toDelete.add(o.getID());
      objectStore.removeAllObjectsByIDNow(toDelete);
      assertFalse(objectStore.containsObject(o.getID()));

      // put should commit the object to the data store.
      objectStore.addNewObject(o);
      objectStore.commitObject(TestPersistenceTransaction.NULL_TRANSACTION, o);
      assertTrue(objectStore.containsObject(o.getID()));
      assertTrue(map.containsKey(o.getID()));
    }

    // removeAll should enqueue an event on the sink to remove all of them.
    objectStore.removeAllObjectsByIDNow(managedIDs);

    // clear the object store...
    for (Iterator i = managed.iterator(); i.hasNext();) {
      ManagedObject o = (ManagedObject) i.next();
      TreeSet toDelete = new TreeSet();
      toDelete.add(o.getID());
      objectStore.removeAllObjectsByIDNow(toDelete);
      assertFalse(objectStore.containsObject(o.getID()));
      assertFalse(map.containsKey(o.getID()));
    }

  }
}
