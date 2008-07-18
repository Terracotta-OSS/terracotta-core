/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.Filter;
//import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollector;
//import com.tc.objectserver.core.api.GarbageCollectorEventListener;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import com.tc.util.ObjectIDSet;

//import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

public class MarkAndSweepGarbageCollectorTest extends TestCase {
  protected long                           objectIDCounter     = 0;
  protected TestManagedObject              root1;
  protected TestManagedObject              root2;
  protected Set                            roots               = new HashSet();
  protected Map                            managed;                                                       // = new
  // HashMap();
  protected GarbageCollector               collector;                                                     // = new
  // MarkAndSweepGarbageCollector();
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

  /**
  public void testGCStats() {
    TestGCStatsEventListener listener = new TestGCStatsEventListener();
    collector.start();
    collector.addGCStatsEventListener(listener);
    collector.gc();
    collector.stop();
    assertEquals(4, listener.getUpdateGCStatusList().size());
    List updateGCStatusList = listener.getUpdateGCStatusList();
    assertEquals("MARK", updateGCStatusList.get(0));
    assertEquals("PAUSE", updateGCStatusList.get(1));
    assertEquals("MARK_COMPLETE", updateGCStatusList.get(2));
    assertEquals("DELETE", updateGCStatusList.get(3));
  public Set<ObjectID> getRootIds() {
    HashSet<ObjectID> rv = new HashSet<ObjectID>();
    for (final ManagedObject root : roots) {
      rv.add(root.getID());
    }
    return rv;
  }
 **/
  
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
  
  /**
  private static class TestGarbageCollectorEventListener implements GarbageCollectorEventListener {
    
    protected List completedList = new ArrayList();
    
    protected List cycleCompletedList = new ArrayList();
    
    protected List deleteList = new ArrayList();
    
    protected List markCompleteList = new ArrayList();
    
    protected List  
    
    public void garbageCollectorCompleted(GarbageCollectionInfo info) {
      //do nothing
    }

    public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
     //
    }

    public void garbageCollectorDelete(GarbageCollectionInfo info) {
     //
    }

    public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorMark(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
     //
    }

    public void garbageCollectorPaused(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorPausing(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorRescue1(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorRescue1Results(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorRescue2(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorRescue2Results(GarbageCollectionInfo info) {
      //
    }

    public void garbageCollectorStart(GarbageCollectionInfo info) {
      //
    }
    
  }
  **/
/**
  private static class TestGCStatsEventListener implements GCStatsEventListener {

    private List updateGCStatusList = new ArrayList();

    public void update(GCStats stats) {
      System.out.println("update:" + stats);
      updateGCStatusList.add(stats.getStatus());
    }

    public List getUpdateGCStatusList() {
      return updateGCStatusList;
    }
  }
**/
  
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
   
//      GCStatsImpl gcStats = resultContext.getGCStats();
//      gcStats.setDeleteState();
//      gcStats.setDeleteStageTime(System.currentTimeMillis() - startTime);
//      gcStats.setCompleteState();
    }

  }

}
