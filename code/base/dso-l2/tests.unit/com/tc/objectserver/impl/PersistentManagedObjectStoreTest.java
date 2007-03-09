/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class PersistentManagedObjectStoreTest extends TestCase {

  private Map                          map;
  private TestManagedObjectPersistor   persistor;
  private PersistentManagedObjectStore objectStore;

  protected void setUp() throws Exception {
    super.setUp();
    map = new HashMap();
    persistor = new TestManagedObjectPersistor(map);
    objectStore = new PersistentManagedObjectStore(persistor);
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
    Collection managedIDs = new ArrayList();
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
      Collection toDelete = new HashSet();
      toDelete.add(o.getID());
      objectStore.removeAllObjectsByIDNow(null, toDelete);
      assertFalse(objectStore.containsObject(o.getID()));

      // put should commit the object to the data store.
      objectStore.addNewObject(o);
      objectStore.commitObject(TestPersistenceTransaction.NULL_TRANSACTION, o);
      assertTrue(objectStore.containsObject(o.getID()));
      assertTrue(map.containsKey(o.getID()));
    }

    // removeAll should enqueue an event on the sink to remove all of them.
    objectStore.removeAllObjectsByIDNow(null, managedIDs);

    // clear the object store...
    for (Iterator i = managed.iterator(); i.hasNext();) {
      ManagedObject o = (ManagedObject) i.next();
      Collection toDelete = new HashSet();
      toDelete.add(o.getID());
      objectStore.removeAllObjectsByIDNow(null, toDelete);
      assertFalse(objectStore.containsObject(o.getID()));
      assertFalse(map.containsKey(o.getID()));
    }

  }

  private static class TestManagedObjectPersistor implements ManagedObjectPersistor {

    public final NoExceptionLinkedQueue loadByObjectIDCalls = new NoExceptionLinkedQueue();
    public final Map                    map;
    public boolean                      closeCalled         = false;
    public SyncObjectIdSet              allObjectIDs        = new SyncObjectIdSet();

    public TestManagedObjectPersistor(Map map) {
      this.map = map;
    }

    public ManagedObject loadObjectByID(ObjectID id) {
      loadByObjectIDCalls.put(id);
      return (ManagedObject) map.get(id);
    }

    public ObjectID loadRootID(String name) {
      return null;
    }

    public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
      return;
    }

    public void saveObject(PersistenceTransaction tx, ManagedObject managedObject) {
      map.put(managedObject.getID(), managedObject);
    }

    public void saveAllObjects(PersistenceTransaction tx, Collection managed) {
      for (Iterator i = managed.iterator(); i.hasNext();) {
        saveObject(tx, (ManagedObject) i.next());
      }
    }

    public void deleteObjectByID(PersistenceTransaction tx, ObjectID id) {
      map.remove(id);
    }

    public void prettyPrint(PrettyPrinter out) {
      return;
    }

    public Set loadRoots() {
      return null;
    }

    public Set loadRootNames() {
      return null;
    }

    public long nextObjectIDBatch(int batchSize) {
      throw new RuntimeException("Implement Me");
    }

    public SyncObjectIdSet getAllObjectIDs() {
      return allObjectIDs;
    }

    public void deleteAllObjectsByID(PersistenceTransaction tx, Collection ids) {
      for (Iterator i = ids.iterator(); i.hasNext();) {
        deleteObjectByID(tx, (ObjectID) i.next());
      }
    }

  }
}
