/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.impl.GCTestObjectManager;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.dgc.impl.GarbageCollectionInfoPublisherImpl;
import com.tc.objectserver.dgc.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.inmemory.NullPersistenceTransactionProvider;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.sequence.DGCSequenceProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class GCStatsEventPublisherTest extends TestCase {
  protected long                           objectIDCounter     = 0;
  protected TestManagedObject              root1;
  protected TestManagedObject              root2;
  protected GarbageCollector               collector;
  protected Set                            lookedUp;
  protected Set                            released;
  protected GCTestObjectManager            objectManager;
  protected PersistenceTransactionProvider transactionProvider = new NullPersistenceTransactionProvider();

  public GCStatsEventPublisherTest(String arg0) {
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
    GarbageCollectionInfoPublisher gcPublisher = new GarbageCollectionInfoPublisherImpl();
    this.collector = new MarkAndSweepGarbageCollector(new ObjectManagerConfig(300000, true, true, true, true, 60000,
                                                                              1000), this.objectManager,
                                                      new TestClientStateManager(), gcPublisher,
                                                      new DGCSequenceProvider(new TestMutableSequence()));
    this.objectManager.setPublisher(gcPublisher);
    this.objectManager.setGarbageCollector(this.collector);
    this.objectManager.start();
    this.root1 = createObject(8);
    this.root2 = createObject(8);
    this.objectManager.createRoot("root1", this.root1.getID());
    this.objectManager.createRoot("root2", this.root2.getID());
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

  private ObjectID nextID() {
    return new ObjectID(this.objectIDCounter++);
  }

  public void testGarbageCollectorListener() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    this.root1.setReference(0, tmo1.getID());

    TestGarbageCollectionInfoCallsListener listener = new TestGarbageCollectionInfoCallsListener();
    this.collector.addListener(listener);
    this.collector.start();
    this.collector.doGC(GCType.FULL_GC);
    this.collector.stop();
    assertEquals(1, listener.startList.size());
    assertEquals(1, listener.markList.size());
    assertEquals(1, listener.markResultsList.size());
    assertEquals(1, listener.rescue1CompleteList.size());
    assertEquals(1, listener.pausingList.size());
    assertEquals(1, listener.pausedList.size());
    assertEquals(1, listener.rescue2StartList.size());
    assertEquals(1, listener.markCompleteList.size());
    assertEquals(1, listener.completedList.size());
    assertEquals(1, listener.cycleCompletedList.size());

  }

  public void testGarbageCollectorListenerShortCircuit() {

    TestGarbageCollectionInfoCallsListener listener = new TestGarbageCollectionInfoCallsListener();
    this.collector.addListener(listener);
    this.collector.start();
    this.collector.doGC(GCType.FULL_GC);
    this.collector.stop();
    assertEquals(1, listener.startList.size());
    assertEquals(1, listener.markList.size());
    assertEquals(1, listener.markResultsList.size());
    assertEquals(1, listener.rescue1CompleteList.size());
    assertEquals(1, listener.completedList.size());
    assertEquals(1, listener.cycleCompletedList.size());

  }

  public void testLastGarbageCollectorStats() {
    GCStatsEventPublisher gcEventPublisher = new GCStatsEventPublisher();
    GarbageCollectionInfo gcInfo1 = new GarbageCollectionInfo(new GarbageCollectionID(0, "xyz0"),
                                                              GarbageCollectionInfo.Type.FULL_GC);
    gcEventPublisher.garbageCollectorStart(gcInfo1);
    GarbageCollectionInfo gcInfo2 = new GarbageCollectionInfo(new GarbageCollectionID(1, "xyz1"),
                                                              GarbageCollectionInfo.Type.FULL_GC);
    gcEventPublisher.garbageCollectorStart(gcInfo2);
    GarbageCollectionInfo gcInfo3 = new GarbageCollectionInfo(new GarbageCollectionID(2, "xyz2"),
                                                              GarbageCollectionInfo.Type.FULL_GC);
    gcEventPublisher.garbageCollectorStart(gcInfo3);

    Assert.assertEquals(gcInfo3.getIteration(), gcEventPublisher.getLastGarbageCollectorStats().getIteration());
  }

  private static class TestGarbageCollectionInfoCallsListener extends TestGarbageCollectorEventListener {

    @Override
    public void garbageCollectorStart(GarbageCollectionInfo info) {
      super.garbageCollectorStart(info);
      assertFalse(info.getStartTime() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

    @Override
    public void garbageCollectorMark(GarbageCollectionInfo info) {
      super.garbageCollectorMark(info);
      assertFalse(info.getBeginObjectCount() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

    @Override
    public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
      super.garbageCollectorMarkResults(info);
      assertFalse(info.getPreRescueCount() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

    @Override
    public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
      super.garbageCollectorRescue1Complete(info);
      assertFalse(info.getRescue1Count() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

    @Override
    public void garbageCollectorPausing(GarbageCollectionInfo info) {
      super.garbageCollectorPausing(info);
      assertFalse(info.getMarkStageTime() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

    @Override
    public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
      super.garbageCollectorRescue2Start(info);
      assertFalse(info.getCandidateGarbageCount() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

    @Override
    public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
      super.garbageCollectorMarkComplete(info);
      assertFalse(info.getPausedStageTime() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

    @Override
    public void garbageCollectorCompleted(GarbageCollectionInfo info) {
      super.garbageCollectorCompleted(info);
      assertFalse(info.getElapsedTime() == GarbageCollectionInfo.NOT_INITIALIZED);
      assertFalse(info.getDeleteStageTime() == GarbageCollectionInfo.NOT_INITIALIZED);
    }

  }

  private static class TestGarbageCollectorEventListener implements GarbageCollectorEventListener {

    protected List startList           = new ArrayList();

    protected List markList            = new ArrayList();

    protected List markResultsList     = new ArrayList();

    protected List rescue1CompleteList = new ArrayList();

    protected List pausingList         = new ArrayList();

    protected List pausedList          = new ArrayList();

    protected List rescue2StartList    = new ArrayList();

    protected List markCompleteList    = new ArrayList();

    protected List deleteList          = new ArrayList();

    protected List cycleCompletedList  = new ArrayList();

    protected List completedList       = new ArrayList();

    protected List cancelList          = new ArrayList();

    public void garbageCollectorStart(GarbageCollectionInfo info) {
      this.startList.add(info);
    }

    public void garbageCollectorMark(GarbageCollectionInfo info) {
      this.markList.add(info);
    }

    public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
      this.markResultsList.add(info);
    }

    public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
      this.rescue1CompleteList.add(info);
    }

    public void garbageCollectorPausing(GarbageCollectionInfo info) {
      this.pausingList.add(info);
    }

    public void garbageCollectorPaused(GarbageCollectionInfo info) {
      this.pausedList.add(info);
    }

    public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
      this.rescue2StartList.add(info);
    }

    public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
      this.markCompleteList.add(info);
    }

    public void garbageCollectorDelete(GarbageCollectionInfo info) {
      this.deleteList.add(info);
    }

    public void garbageCollectorCycleCompleted(GarbageCollectionInfo info, ObjectIDSet toDelete) {
      this.cycleCompletedList.add(info);
    }

    public void garbageCollectorCompleted(GarbageCollectionInfo info) {
      this.completedList.add(info);
    }

    public void garbageCollectorCanceled(GarbageCollectionInfo info) {
      this.cancelList.add(info);
    }

  }

}
