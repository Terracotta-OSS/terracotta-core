/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.impl.GCTestObjectManager;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.inmemory.NullPersistenceTransactionProvider;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.sequence.DGCSequenceProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class MarkAndSweepGarbageCollectorTest extends TestCase {
  protected long                           objectIDCounter     = 0;
  protected TestManagedObject              root1;
  protected TestManagedObject              root2;
  protected MarkAndSweepGarbageCollector   collector;
  protected Set                            lookedUp;
  protected Set                            released;
  protected GCTestObjectManager            objectManager;
  protected ClientStateManager             stateManager;
  protected PersistenceTransactionProvider transactionProvider = new NullPersistenceTransactionProvider();

  private final Filter                     filter              = new Filter() {
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
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.lookedUp = new HashSet<ObjectID>();
    this.released = new HashSet<ObjectID>();
    this.objectManager = new GCTestObjectManager(this.lookedUp, this.released, this.transactionProvider);
    this.stateManager = new TestClientStateManager();
    this.collector = new MarkAndSweepGarbageCollector(new ObjectManagerConfig(300000, true, true, true, true, 60000,
                                                                              1000), this.objectManager,
                                                      this.stateManager, new GarbageCollectionInfoPublisherImpl(),
                                                      new DGCSequenceProvider(new TestMutableSequence()));
    this.objectManager.setGarbageCollector(this.collector);
    GarbageCollectionInfoPublisher gcPublisher = new GarbageCollectionInfoPublisherImpl();
    this.objectManager.setPublisher(gcPublisher);
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
    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager),
                                          this.filter, this.objectManager.getRootIDs(), this.objectManager
                                              .getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testOneLevelNoGarbage() {
    TestManagedObject tmo = createObject(3);
    this.root1.setReference(0, tmo.getID());
    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager),
                                          this.filter, this.objectManager.getRootIDs(), this.objectManager
                                              .getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testSharedBetweenRootsNoGarbage() {
    TestManagedObject tmo = createObject(3);
    this.root1.setReference(0, tmo.getID());
    this.root2.setReference(0, tmo.getID());
    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager),
                                          this.filter, this.objectManager.getRootIDs(), this.objectManager
                                              .getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleNoGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    this.root1.setReference(0, tmo1.getID());

    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager),
                                          this.filter, this.objectManager.getRootIDs(), this.objectManager
                                              .getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleWithGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    this.root1.setReference(0, tmo1.getID());

    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager),
                                          this.filter, this.objectManager.getRootIDs(), this.objectManager
                                              .getAllObjectIDs());
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

    this.root1.setReference(0, tmo1.getID());

    Filter testFilter = new Filter() {
      public boolean shouldVisit(ObjectID referencedObject) {
        return (!tmo2.getID().equals(referencedObject));
      }
    };

    // make sure that the filter filters out the sub-graph starting at the reference to tmo2.
    this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager), testFilter,
                           this.objectManager.getRootIDs(), this.objectManager.getAllObjectIDs());
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertFalse(this.lookedUp.contains(tmo2.getID()));
    assertFalse(this.lookedUp.contains(tmo3.getID()));

    // try it with the regular filter to make sure the behavior is actually different.
    this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager), this.filter,
                           this.objectManager.getRootIDs(), this.objectManager.getAllObjectIDs());
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

    this.root1.setReference(0, tmo1.getID());

    this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager), this.filter,
                           this.objectManager.getRootIDs(), this.objectManager.getAllObjectIDs());
    assertTrue(this.lookedUp.equals(this.released));
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
    assertFalse(this.collector.isPausingOrPaused());
    this.collector.requestGCPause();
    this.collector.notifyReadyToGC();
    assertTrue(this.collector.isPausingOrPaused());
    this.collector.notifyGCComplete();
    assertFalse(this.collector.isPausingOrPaused());
  }

  public void testYoungGenGC() throws Exception {
    // Create a loop
    final TestManagedObject tmo1 = createObject(3);
    createLoopFrom(tmo1, 10);
    this.root1.setReference(0, tmo1.getID());

    this.collector.doGC(GCType.YOUNG_GEN_GC);
    Set deleted = this.objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // Create a loop 2
    final TestManagedObject tmo2 = createObject(3);
    createLoopFrom(tmo2, 10);
    this.root1.setReference(1, tmo2.getID());

    // Create a loop 3
    final TestManagedObject tmo3 = createObject(3);
    createLoopFrom(tmo3, 10);
    this.root1.setReference(2, tmo3.getID());

    this.collector.doGC(GCType.YOUNG_GEN_GC);
    deleted = this.objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // evict tmo1, still youngGen returns no garbage
    this.objectManager.evict(tmo1.getID());
    this.collector.doGC(GCType.YOUNG_GEN_GC);
    deleted = this.objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // now make the first loop garbage, still young gen returns no garbage
    this.root1.setReference(0, ObjectID.NULL_ID);
    this.collector.doGC(GCType.YOUNG_GEN_GC);
    deleted = this.objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // now make the second loop garbage
    this.root1.setReference(1, ObjectID.NULL_ID);
    this.collector.doGC(GCType.YOUNG_GEN_GC);
    deleted = this.objectManager.getGCedObjectIDs();
    assertFalse(deleted.isEmpty());
    assertTrue(deleted.contains(tmo2.getID()));
    assertEquals(11, deleted.size());

    // perform yet another young generation GC, but no garbage
    this.collector.doGC(GCType.YOUNG_GEN_GC);
    deleted = this.objectManager.getGCedObjectIDs();
    assertTrue(deleted.isEmpty());

    // perform full GC and find the garbage
    this.collector.doGC(GCType.FULL_GC);
    deleted = this.objectManager.getGCedObjectIDs();
    assertFalse(deleted.isEmpty());
    assertTrue(deleted.contains(tmo1.getID()));
    assertEquals(11, deleted.size());

  }

  private ObjectID nextID() {
    return new ObjectID(this.objectIDCounter++);
  }

  private TestManagedObject createObject(int refCount) {
    ArrayList<ObjectID> ids = new ArrayList<ObjectID>(refCount);

    for (int i = 0; i < refCount; i++) {
      ids.add(ObjectID.NULL_ID);
    }

    TestManagedObject tmo = new TestManagedObject(nextID(), ids);
    this.objectManager.createObject(tmo.getID(), tmo.getReference());
    return tmo;
  }
}
