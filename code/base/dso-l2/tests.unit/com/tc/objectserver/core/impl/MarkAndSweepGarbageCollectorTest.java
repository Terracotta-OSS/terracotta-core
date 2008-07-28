/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class MarkAndSweepGarbageCollectorTest extends TestCase {
  protected long                           objectIDCounter     = 0;
  protected TestManagedObject              root1;
  protected TestManagedObject              root2;
  protected GarbageCollector               collector;
  protected Set                            lookedUp;
  protected Set                            released;
  protected GCTestObjectManager            objectManager;
  protected PersistenceTransactionProvider transactionProvider = new NullPersistenceTransactionProvider();

  private Filter                           filter              = new Filter() {
                                                                 public boolean shouldVisit(ObjectID referencedObject) {
                                                                   return true;
                                                                 }
                                                               };

  /**
   * Constructor for MarkAndSweepGarbageCollectorTest.
   * 
   * @param arg0
   */
  public MarkAndSweepGarbageCollectorTest(String arg0) {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    this.lookedUp = new HashSet<ObjectID>();
    this.released = new HashSet<ObjectID>();
    this.objectManager = new GCTestObjectManager(lookedUp, released, transactionProvider);
    this.collector = new MarkAndSweepGarbageCollector(this.objectManager, new TestClientStateManager(),
                                                      new ObjectManagerConfig(300000, true, true, true, true, 60000));
    this.objectManager.setGarbageCollector(collector);
    this.objectManager.start();
    this.root1 = createObject(8);
    this.root2 = createObject(8);
    this.objectManager.createRoot("root1", this.root1.getID());
    this.objectManager.createRoot("root2", this.root2.getID());
  }

  public Object getLock() {
    return this;
  }

  public void testEmptyRoots() {
    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testOneLevelNoGarbage() {
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testSharedBetweenRootsNoGarbage() {
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    root2.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleNoGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleWithGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 1);
  }

  public void testFilter() {
    final TestManagedObject tmo1 = createObject(3);
    final TestManagedObject tmo2 = createObject(3);
    final TestManagedObject tmo3 = createObject(3);
    // Adding the 4'th object to make sure that GC collect() doesn't short circuit collection cycle since there is no
    // garbage.
    final TestManagedObject tmo4 = createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());
    tmo2.setReference(1, tmo3.getID());
    tmo3.setReference(0, tmo4.getID());

    root1.setReference(0, tmo1.getID());

    Filter testFilter = new Filter() {
      public boolean shouldVisit(ObjectID referencedObject) {
        return (!tmo2.getID().equals(referencedObject));
      }
    };

    // make sure that the filter filters out the sub-graph starting at the reference to tmo2.
    collector.collect(testFilter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertFalse(this.lookedUp.contains(tmo2.getID()));
    assertFalse(this.lookedUp.contains(tmo3.getID()));

    // try it with the regular filter to make sure the behavior is actually different.
    collector.collect(filter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertTrue(this.lookedUp.contains(tmo2.getID()));
    assertTrue(this.lookedUp.contains(tmo3.getID()));
  }

  public void testLookupAndReleaseBalanced() {

    final TestManagedObject tmo1 = createObject(3);
    final TestManagedObject tmo2 = createObject(3);
    final TestManagedObject tmo3 = createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());
    tmo2.setReference(1, tmo3.getID());

    root1.setReference(0, tmo1.getID());

    collector.collect(filter, objectManager.getRootIDs(), objectManager.getAllObjectIDs());
    assertTrue(lookedUp.equals(released));
  }

  private void createLoopFrom(TestManagedObject tmo, int count) {
    TestManagedObject prev = tmo;
    while (count-- > 0) {
      final TestManagedObject next = createObject(3);
      prev.setReference(0, next.getID());
      prev = next;
    }
    prev.setReference(0, tmo.getID());
  }

  public void testIsInGCPause() throws Exception {
    assertFalse(collector.isPausingOrPaused());
    collector.requestGCPause();
    collector.notifyReadyToGC();
    assertTrue(collector.isPausingOrPaused());
    collector.notifyGCComplete();
    assertFalse(collector.isPausingOrPaused());
  }

  public void testYoungGenGC() throws Exception {
    // Create a loop
    final TestManagedObject tmo1 = createObject(3);
    createLoopFrom(tmo1, 10);
    root1.setReference(0, tmo1.getID());

    collector.gcYoung();
    Set deleted = objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // Create a loop 2
    final TestManagedObject tmo2 = createObject(3);
    createLoopFrom(tmo2, 10);
    root1.setReference(1, tmo2.getID());

    // Create a loop 3
    final TestManagedObject tmo3 = createObject(3);
    createLoopFrom(tmo3, 10);
    root1.setReference(2, tmo3.getID());

    collector.gcYoung();
    deleted = objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // evict tmo1, still youngGen returns no garbage
    objectManager.evict(tmo1.getID());
    collector.gcYoung();
    deleted = objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // now make the first loop garbage, still young gen returns no garbage
    root1.setReference(0, ObjectID.NULL_ID);
    collector.gcYoung();
    deleted = objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // now make the second loop garbage
    root1.setReference(1, ObjectID.NULL_ID);
    collector.gcYoung();
    deleted = objectManager.getGCedObjectIDs();
    assertFalse(deleted.isEmpty());
    assertTrue(deleted.contains(tmo2.getID()));
    assertEquals(11, deleted.size());

    // perform yet another young generation GC, but no garbage
    collector.gcYoung();
    deleted = objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // perform full GC and find the garbage
    collector.gc();
    deleted = objectManager.getGCedObjectIDs();
    assertFalse(deleted.isEmpty());
    assertTrue(deleted.contains(tmo1.getID()));
    assertEquals(11, deleted.size());

  }

  private ObjectID nextID() {
    return new ObjectID(objectIDCounter++);
  }

  private TestManagedObject createObject(int refCount) {
    ObjectID[] ids = new ObjectID[refCount];

    Arrays.fill(ids, ObjectID.NULL_ID);

    TestManagedObject tmo = new TestManagedObject(nextID(), ids);
    objectManager.createObject(tmo.getID(), tmo.getReference());
    return tmo;
  }
}
