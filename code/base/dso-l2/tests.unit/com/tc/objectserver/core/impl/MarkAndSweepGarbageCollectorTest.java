/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.Filter; 
import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollectionInfoFactory;

import com.tc.objectserver.core.api.GarbageCollector; 
import com.tc.objectserver.core.api.GarbageCollectorEventListener; 
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.handler.GarbageDisposeHandler;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;

import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;

import junit.framework.TestCase;

public class MarkAndSweepGarbageCollectorTest extends TestCase {
  protected long                           objectIDCounter     = 0;
  protected TestManagedObject              root1;
  protected TestManagedObject              root2;
  protected Set                            roots               = new HashSet();
  protected Map                            managed;
  protected GarbageCollector               collector;
  protected Set                            lookedUp;
  protected Set                            released;
  protected ObjectManager                  objectManager;
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
    this.managed = new HashMap<ObjectID, ManagedObjectReference>();
    this.objectManager = new GCTestObjectManager();
    this.collector = new MarkAndSweepGarbageCollector(this.objectManager, new TestClientStateManager(), false);
    this.lookedUp = new HashSet<ObjectID>();
    this.released = new HashSet<ObjectID>();
    this.root1 = createObject(8);
    this.root2 = createObject(8);
    this.roots = new HashSet<ManagedObject>();
    roots.add(root1);
    roots.add(root2);
  }

  public Object getLock() {
    return this;
  }

  public void testGarbageCollectionInfoCalls() {
    TestGarbageCollectionInfoFactory factory = new TestGarbageCollectionInfoFactory();
    this.collector.setGarbageCollectionInfoFactory(factory);
    TestGarbageCollectionInfoCallsListener listener = new TestGarbageCollectionInfoCallsListener();
    this.collector.addListener(listener);
    collector.start();
    collector.gc();
    collector.stop();

    assertEquals(1, listener.startList.size());
    assertEquals(1, listener.markList.size());
    assertEquals(1, listener.markResultsList.size());
    assertEquals(1, listener.rescue1CompleteList.size());
    assertEquals(1, listener.pausingList.size());
    assertEquals(1, listener.pausedList.size());
    assertEquals(1, listener.rescue2StartList.size());
    assertEquals(1, listener.markCompleteList.size());
    assertEquals(1, listener.deleteList.size());
    assertEquals(1, listener.completedList.size());
    assertEquals(1, listener.cycleCompletedList.size());

  }

  private static class TestGarbageCollectionInfoCallsListener extends TestGarbageCollectorEventListener {

    @Override
    public void garbageCollectorStart(GarbageCollectionInfo info) {
      super.garbageCollectorStart(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(2, gcInfo.setCallStack.size());
      assertEquals("setStartTime", gcInfo.setCallStack.pop());
      assertEquals("markFullGen", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorMark(GarbageCollectionInfo info) {
      super.garbageCollectorMark(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(1, gcInfo.setCallStack.size());
      assertEquals("setBeginObjectCount", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
      super.garbageCollectorMarkResults(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(1, gcInfo.setCallStack.size());
      assertEquals("setPreRescueCount", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
      super.garbageCollectorRescue1Complete(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(1, gcInfo.setCallStack.size());
      assertEquals("setRescue1Count", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorPausing(GarbageCollectionInfo info) {
      super.garbageCollectorPausing(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(1, gcInfo.setCallStack.size());
      assertEquals("setMarkStageTime", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorPaused(GarbageCollectionInfo info) {
      super.garbageCollectorPaused(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(0, gcInfo.setCallStack.size());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
      super.garbageCollectorRescue2Start(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(1, gcInfo.setCallStack.size());
      assertEquals("setCandidateGarbageCount", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
      super.garbageCollectorMarkComplete(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(3, gcInfo.setCallStack.size());
      assertEquals("setPausedStageTime", gcInfo.setCallStack.pop());
      assertEquals("setDeleted", gcInfo.setCallStack.pop());
      assertEquals("setRescueTimes", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
      super.garbageCollectorCycleCompleted(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(1, gcInfo.setCallStack.size());
      assertEquals("setElapsedTime", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorDelete(GarbageCollectionInfo info) {
      super.garbageCollectorDelete(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(0, gcInfo.setCallStack.size());
      gcInfo.setCallStack.clear();
    }

    @Override
    public void garbageCollectorCompleted(GarbageCollectionInfo info) {
      super.garbageCollectorCompleted(info);
      TestGarbageCollectionInfo gcInfo = (TestGarbageCollectionInfo) info;
      assertEquals(2, gcInfo.setCallStack.size());
      assertEquals("setElapsedTime", gcInfo.setCallStack.pop());
      assertEquals("setDeleteStageTime", gcInfo.setCallStack.pop());
      gcInfo.setCallStack.clear();
    }

  }

  public void testGarbageCollectorListener() {
    TestGarbageCollectorEventListener listener = new TestGarbageCollectorEventListener();
    collector.addListener(listener);
    collector.start();
    collector.gc();
    collector.stop();
    assertEquals(1, listener.startList.size());
    assertEquals(1, listener.markList.size());
    assertEquals(1, listener.markResultsList.size());
    assertEquals(1, listener.rescue1CompleteList.size());
    assertEquals(1, listener.pausingList.size());
    assertEquals(1, listener.pausedList.size());
    assertEquals(1, listener.rescue2StartList.size());
    assertEquals(1, listener.markCompleteList.size());
    assertEquals(1, listener.deleteList.size());
    assertEquals(1, listener.completedList.size());
    assertEquals(1, listener.cycleCompletedList.size());

  }

  public void testGCStatsEventListener() {
    TestGCStatsEventListener listener = new TestGCStatsEventListener();

    GCStatsEventPublisher gcStatsGarbageCollectionListener = new GCStatsEventPublisher();
    gcStatsGarbageCollectionListener.addListener(listener);
    collector.addListener(gcStatsGarbageCollectionListener);
    collector.start();
    collector.gc();
    collector.stop();
    
    assertEquals(6, listener.gcStatsList.size());
  }

  public void testEmptyRoots() {
    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(toDelete.size() == 0);
  }

  public void testOneLevelNoGarbage() {
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(toDelete.size() == 0);
  }

  public void testSharedBetweenRootsNoGarbage() {
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    root2.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleNoGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleWithGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(toDelete.size() == 1);
  }

  public void testFilter() {
    final TestManagedObject tmo1 = createObject(3);
    final TestManagedObject tmo2 = createObject(3);
    final TestManagedObject tmo3 = createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());
    tmo2.setReference(1, tmo3.getID());

    root1.setReference(0, tmo1.getID());

    Filter testFilter = new Filter() {
      public boolean shouldVisit(ObjectID referencedObject) {
        return (!tmo2.getID().equals(referencedObject));
      }
    };

    // make sure that the filter filters out the sub-graph starting at the reference to tmo2.
    collector.collect(testFilter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertFalse(this.lookedUp.contains(tmo2.getID()));
    assertFalse(this.lookedUp.contains(tmo3.getID()));

    // try it with the regular filter to make sure the behavior is actually different.
    collector.collect(filter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
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

    collector.collect(filter, objectManager.getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(lookedUp.equals(released));
  }

  public void testIsInGCPause() throws Exception {
    assertFalse(collector.isPausingOrPaused());
    collector.requestGCPause();
    collector.notifyReadyToGC();
    assertTrue(collector.isPausingOrPaused());
    collector.notifyGCComplete();
    assertFalse(collector.isPausingOrPaused());
  }

  private ObjectID nextID() {
    return new ObjectID(objectIDCounter++);
  }

  private TestManagedObject createObject(int refCount) {
    ObjectID[] ids = new ObjectID[refCount];

    Arrays.fill(ids, ObjectID.NULL_ID);

    TestManagedObject tmo = new TestManagedObject(nextID(), ids);
    managed.put(tmo.getID(), tmo.getReference());
    return tmo;
  }

  private static class TestGCStatsEventListener implements GCStatsEventListener {

    protected List gcStatsList = new ArrayList();

    /**
     * notify the listener that GCStats object has been updated
     * 
     * @param stats statistics about this collection
     */
    public void update(GCStats stats) {
      gcStatsList.add(stats);
    }

  }

  private static class TestGarbageCollectionInfoFactory implements GarbageCollectionInfoFactory {

    public GarbageCollectionInfo newInstance(int iteration) {
      return new TestGarbageCollectionInfo(iteration);
    }

  }

  private static class TestGarbageCollectionInfo implements GarbageCollectionInfo {

    private int       actualGarbageCount;

    private int       beginObjectCount;

    private int       candiateGarbageCount;

    private long      startTime;

    private long      deleteStageTime;

    private SortedSet deleted;

    private long      elapsedTime;

    private int       iteration;

    private long      markStageTime;

    private Object    object;

    private long      pausedStageTime;

    private int       preRescueCount;

    private int       rescue1Count;

    private List      rescueTimes;

    private boolean   youngGen;

    protected Stack   setCallStack = new Stack();

    public TestGarbageCollectionInfo(int iteration) {
      this.iteration = iteration;
    }

    public int getActualGarbageCount() {
      return actualGarbageCount;
    }

    public int getBeginObjectCount() {
      return beginObjectCount;
    }

    public int getCandidateGarbageCount() {
      return candiateGarbageCount;
    }

    public long getDeleteStageTime() {
      return deleteStageTime;
    }

    public SortedSet getDeleted() {
      return deleted;
    }

    public long getElapsedTime() {
      return elapsedTime;
    }

    public int getIteration() {
      return iteration;
    }

    public long getMarkStageTime() {
      return markStageTime;
    }

    public Object getObject() {
      return object;
    }

    public long getPausedStageTime() {
      return pausedStageTime;
    }

    public int getPreRescueCount() {
      return preRescueCount;
    }

    public int getRescue1Count() {
      return rescue1Count;
    }

    public List getRescueTimes() {
      return rescueTimes;
    }

    public long getStartTime() {
      return startTime;
    }

    public boolean isYoungGen() {
      return youngGen;
    }

    public void setDeleteStageTime(long time) {
      setCallStack.add("setDeleteStageTime");
      this.deleteStageTime = time;
    }

    public void setElapsedTime(long time) {
      setCallStack.add("setElapsedTime");
      this.elapsedTime = time;
    }

    public void setObject(Object stats) {
      this.object = stats;
    }

    public void markFullGen() {
      setCallStack.add("markFullGen");
      youngGen = false;
    }

    public void markYoungGen() {
      setCallStack.add("markYounGen");
      youngGen = true;
    }

    public void setBeginObjectCount(int count) {
      setCallStack.add("setBeginObjectCount");
      this.beginObjectCount = count;
    }

    public void setCandidateGarbageCount(int count) {
      setCallStack.add("setCandidateGarbageCount");
      this.candiateGarbageCount = count;
    }

    public void setDeleted(SortedSet deleted) {
      setCallStack.add("setDeleted");
      this.deleted = deleted;
    }

    public void setMarkStageTime(long time) {
      setCallStack.add("setMarkStageTime");
      this.markStageTime = time;
    }

    public void setPausedStageTime(long time) {
      setCallStack.add("setPausedStageTime");
      this.pausedStageTime = time;
    }

    public void setPreRescueCount(int count) {
      setCallStack.add("setPreRescueCount");
      this.preRescueCount = count;
    }

    public void setRescue1Count(int count) {
      setCallStack.add("setRescue1Count");
      this.rescue1Count = count;
    }

    public void setRescueTimes(List rescueTimes) {
      setCallStack.add("setRescueTimes");
      this.rescueTimes = rescueTimes;
    }

    public void setStartTime(long time) {
      setCallStack.add("setStartTime");
      this.startTime = time;
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

    public void garbageCollectorStart(GarbageCollectionInfo info) {
      startList.add(info);
    }

    public void garbageCollectorMark(GarbageCollectionInfo info) {
      markList.add(info);
    }

    public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
      markResultsList.add(info);
    }

    public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
      rescue1CompleteList.add(info);
    }

    public void garbageCollectorPausing(GarbageCollectionInfo info) {
      pausingList.add(info);
    }

    public void garbageCollectorPaused(GarbageCollectionInfo info) {
      pausedList.add(info);
    }

    public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
      rescue2StartList.add(info);
    }

    public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
      markCompleteList.add(info);
    }

    public void garbageCollectorDelete(GarbageCollectionInfo info) {
      deleteList.add(info);
    }

    public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
      cycleCompletedList.add(info);
    }

    public void garbageCollectorCompleted(GarbageCollectionInfo info) {
      completedList.add(info);
    }

  }

  /**
   * private static class TestGCStatsEventListener implements GCStatsEventListener { private List updateGCStatusList =
   * new ArrayList(); public void update(GCStats stats) { System.out.println("update:" + stats);
   * updateGCStatusList.add(stats.getStatus()); } public List getUpdateGCStatusList() { return updateGCStatusList; } }
   */

  private class GCTestObjectManager implements ObjectManager {

    public ManagedObject getObjectByID(ObjectID id) {
      lookedUp.add(id);
      ManagedObjectReference ref = (ManagedObjectReference) managed.get(id);
      return (ref == null) ? null : ref.getObject();
    }

    public void release(PersistenceTransaction tx, ManagedObject object) {
      released.add(object.getID());
      return;
    }

    public void releaseAll(PersistenceTransaction tx, Collection c) {
      return;
    }

    public void stop() {
      throw new ImplementMe();
    }

    public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                                 int maxCount) {
      throw new ImplementMe();
    }

    public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
      throw new ImplementMe();
    }

    public Iterator getRoots() {
      throw new ImplementMe();
    }

    public void createRoot(String name, ObjectID id) {
      throw new ImplementMe();
    }

    public ObjectID lookupRootID(String name) {
      throw new ImplementMe();
    }

    public GarbageCollector getGarbageCollector() {
      throw new ImplementMe();
    }

    public void setGarbageCollector(GarbageCollector gc) {
      throw new ImplementMe();
    }

    public void setStatsListener(ObjectManagerStatsListener listener) {
      throw new ImplementMe();
    }

    public void start() {
      throw new ImplementMe();
    }

    public void releaseReadOnly(ManagedObject object) {
      released.add(object.getID());
      return;
    }

    public void releaseAllReadOnly(Collection objects) {
      releaseAll(transactionProvider.nullTransaction(), objects);
    }

    public int getCheckedOutCount() {
      return 0;
    }

    public ObjectIDSet getAllObjectIDs() {
      return new ObjectIDSet(managed.keySet());
    }

    public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
      throw new ImplementMe();
    }

    public void waitUntilReadyToGC() {
      collector.notifyReadyToGC();
    }

    public Set getRootIDs() {
      HashSet rv = new HashSet();
      for (Iterator i = roots.iterator(); i.hasNext();) {
        ObjectID id = ((TestManagedObject) i.next()).getID();
        rv.add(id);
      }
      return rv;
    }

    public void flushAndEvict(List objects2Flush) {
      throw new ImplementMe();
    }

    public Map getRootNamesToIDsMap() {
      throw new ImplementMe();
    }

    public void preFetchObjectsAndCreate(Set oids, Set newOids) {
      throw new ImplementMe();
    }

    public void createNewObjects(Set ids) {
      throw new ImplementMe();
    }

    public ManagedObject getObjectByIDOrNull(ObjectID id) {
      ManagedObject mo = getObjectByID(id);
      if (mo != null && mo.isNew()) { return null; }
      return mo;
    }

    public void notifyGCComplete(GCResultContext resultContext) {

      GarbageDisposeHandler handler = new GarbageDisposeHandler(new NullManagedObjectPersistor(),
                                                                new NullPersistenceTransactionProvider(), 1);

      handler.handleEvent(resultContext);
    }

  }

  private static class NullManagedObjectPersistor implements ManagedObjectPersistor {

    public Set loadRoots() {
      return new HashSet();
    }

    public Set loadRootNames() {
      return new HashSet();
    }

    public ObjectID loadRootID(String name) {
      return null;
    }

    public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
      //
    }

    public ManagedObject loadObjectByID(ObjectID id) {
      return null;
    }

    public long nextObjectIDBatch(int batchSize) {
      return -1;
    }

    public void setNextAvailableObjectID(long startID) {
      //
    }

    public SyncObjectIdSet getAllObjectIDs() {
      return new SyncObjectIdSetImpl();
    }

    public void saveObject(PersistenceTransaction tx, ManagedObject managedObject) {
      //
    }

    public void saveAllObjects(PersistenceTransaction tx, Collection managed) {
      //
    }

    public void deleteAllObjectsByID(PersistenceTransaction tx, SortedSet<ObjectID> ids) {
      //
    }

    public Map loadRootNamesToIDs() {
      return new HashMap();
    }

    public boolean addMapTypeObject(ObjectID id) {
      throw new UnsupportedOperationException();
    }

    public boolean addNewObject(ObjectID id) {
      throw new UnsupportedOperationException();
    }

    public boolean containsMapType(ObjectID id) {
      throw new UnsupportedOperationException();
    }

    public boolean containsObject(ObjectID id) {
      throw new UnsupportedOperationException();
    }

    public SyncObjectIdSet getAllMapsObjectIDs() {
      throw new UnsupportedOperationException();
    }

    public int getObjectCount() {
      throw new UnsupportedOperationException();
    }

    public void removeAllMapTypeObject(Collection ids) {
      throw new UnsupportedOperationException();
    }

    public void removeAllObjectsByID(SortedSet<ObjectID> ids) {
      throw new UnsupportedOperationException();
    }

    public ObjectIDSet snapshotObjects() {
      throw new UnsupportedOperationException();
    }
  }

  private static class NullPersistenceTransactionProvider implements PersistenceTransactionProvider {

    public PersistenceTransaction newTransaction() {
      return new TestPersistenceTransaction();
    }

    public PersistenceTransaction nullTransaction() {
      return new TestPersistenceTransaction();
    }
  }

}
