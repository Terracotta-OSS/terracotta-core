/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InMemoryManagedObjectStoreTest extends TCTestCase {

  private InMemoryManagedObjectStore os;
  private Map                        managed;

  public void setUp() throws Exception {
    managed = new HashMap();
    os = new InMemoryManagedObjectStore(managed);
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
