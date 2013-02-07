/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.TransactionProvider;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.impl.GCTestObjectManager;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.impl.NullTransactionProvider;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.impl.TestGarbageCollectionManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
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
  protected Set<ObjectID>                  lookedUp;
  protected Set<ObjectID>                  released;
  protected GCTestObjectManager            objectManager;
  protected ClientStateManager             stateManager;
  protected GarbageCollectionManager       garbageCollectionManager;
  protected TransactionProvider transactionProvider = new NullTransactionProvider();

  private final Filter                     filter              = new Filter() {
                                                                 @Override
                                                                public boolean shouldVisit(ObjectID referencedObject) {
                                                                   return true;
                                                                 }
                                                               };

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
    this.garbageCollectionManager = new TestGarbageCollectionManager();
    this.collector = new MarkAndSweepGarbageCollector(new ObjectManagerConfig(300000, true, true, true), this.objectManager,
            this.stateManager, new GarbageCollectionInfoPublisherImpl(), new DGCSequenceProvider(new TestMutableSequence()));
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
    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false),
                                          this.filter, this.objectManager.getRootIDs(),
                                          this.objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testOneLevelNoGarbage() {
    TestManagedObject tmo = createObject(3);
    this.root1.setReference(0, tmo.getID());
    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false),
                                          this.filter, this.objectManager.getRootIDs(),
                                          this.objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testSharedBetweenRootsNoGarbage() {
    TestManagedObject tmo = createObject(3);
    this.root1.setReference(0, tmo.getID());
    this.root2.setReference(0, tmo.getID());
    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false),
                                          this.filter, this.objectManager.getRootIDs(),
                                          this.objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleNoGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    this.root1.setReference(0, tmo1.getID());

    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false),
                                          this.filter, this.objectManager.getRootIDs(),
                                          this.objectManager.getAllObjectIDs());
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleWithGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    this.root1.setReference(0, tmo1.getID());

    Set toDelete = this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false),
                                          this.filter, this.objectManager.getRootIDs(),
                                          this.objectManager.getAllObjectIDs());
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
      @Override
      public boolean shouldVisit(ObjectID referencedObject) {
        return (!tmo2.getID().equals(referencedObject));
      }
    };

    // make sure that the filter filters out the sub-graph starting at the reference to tmo2.
    this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false), testFilter,
                           this.objectManager.getRootIDs(), this.objectManager.getAllObjectIDs());
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertFalse(this.lookedUp.contains(tmo2.getID()));
    assertFalse(this.lookedUp.contains(tmo3.getID()));

    // try it with the regular filter to make sure the behavior is actually different.
    this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false), this.filter,
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

    this.collector.collect(new FullGCHook(this.collector, this.objectManager, this.stateManager, false), this.filter,
                           this.objectManager.getRootIDs(), this.objectManager.getAllObjectIDs());
    assertTrue(this.lookedUp.equals(this.released));
  }

  public void testIsInGCPause() throws Exception {
    assertFalse(this.collector.isPausingOrPaused());
    this.collector.requestGCPause();
    this.collector.notifyReadyToGC();
    assertTrue(this.collector.isPausingOrPaused());
    this.collector.notifyGCComplete();
    assertFalse(this.collector.isPausingOrPaused());
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
