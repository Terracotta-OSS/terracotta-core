/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.persistence.gb.GBManagedObjectPersistor;
import com.tc.objectserver.persistence.gb.GBPersistor;
import com.tc.objectserver.persistence.gb.StorageManagerFactory;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.heap.HeapStorageManager;

public class InMemoryManagedObjectStoreTest extends TCTestCase {

  private PersistentManagedObjectStore os;
  private Map                        managed;

  public void setUp() throws Exception {
    managed = new HashMap();
    os = new PersistentManagedObjectStore(new GBPersistor(new StorageManagerFactory() {

          @Override
          public StorageManager createStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap) {
              return new HeapStorageManager();
          }
      }).getManagedObjectPersistor(),new TestSink());
  }

  public void testReleaseAll() {
    List l = new ArrayList();
    for (int i = 0; i < 10; i++) {
      l.add(new TestManagedObject(new ObjectID(i)));
    }
    assertEquals(0, managed.size());

    os.commitAllObjects(TestPersistenceTransaction.NULL_TRANSACTION, l);

    for (Iterator i = l.iterator(); i.hasNext();) {
      os.addNewObject((ManagedObject) i.next());
    }

    assertTrue(managed.values().containsAll(l));
    assertTrue(l.containsAll(managed.values()));
  }

}
