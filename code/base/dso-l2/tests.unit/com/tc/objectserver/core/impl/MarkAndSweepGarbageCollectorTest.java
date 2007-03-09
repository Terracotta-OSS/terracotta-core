/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import com.tc.text.PrettyPrinter;
import com.tc.util.SyncObjectIdSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class MarkAndSweepGarbageCollectorTest extends TestCase implements ObjectManager {
  private long                           objectIDCounter     = 0;
  private TestManagedObject              root1;
  private TestManagedObject              root2;
  private Set                            roots               = new HashSet();
  private Map                            managed;                                                       // = new
  // HashMap();
  private GarbageCollector               collector;                                                     // = new
  // MarkAndSweepGarbageCollector();
  private Set                            lookedUp;
  private Set                            released;
  private PersistenceTransactionProvider transactionProvider = new NullPersistenceTransactionProvider();

  private Filter                         filter              = new Filter() {
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
    this.managed = new HashMap();

    this.collector = new MarkAndSweepGarbageCollector(this, new TestClientStateManager(), false);
    this.lookedUp = new HashSet();
    this.released = new HashSet();
    this.root1 = createObject(8);
    this.root2 = createObject(8);
    this.roots = new HashSet();
    roots.add(root1);
    roots.add(root2);
  }

  public Object getLock() {
    return this;
  }

  public Set getRootIds() {
    HashSet rv = new HashSet();
    for (Iterator i = roots.iterator(); i.hasNext();) {
      rv.add(((TestManagedObject) i.next()).getID());
    }
    return rv;
  }

  public void testEmptyRoots() {
    // System.out.println("running: testEmptyRoots");

    Set toDelete = collector.collect(filter, getRootIds(), new HashSet(managed.keySet()));
    // System.out.println(toDelete);
    assertTrue(toDelete.size() == 0);
  }

  public void testOneLevelNoGarbage() {
    // System.out.println("running: testOneLevelNoGarbage");
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, getRootIds(), new HashSet(managed.keySet()));
    // System.out.println(toDelete);
    assertTrue(toDelete.size() == 0);
  }

  public void testSharedBetweenRootsNoGarbage() {
    // System.out.println("running: testSharedBetweenRootsNoGarbage");
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    root2.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, getRootIds(), new HashSet(managed.keySet()));
    // System.out.println(toDelete);
    // System.out.println(managed);
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleNoGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, getRootIds(), new HashSet(managed.keySet()));
    // System.out.println(toDelete);
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleWithGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, getRootIds(), new HashSet(managed.keySet()));
    // System.out.println(toDelete);
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
        boolean rv = (!tmo2.getID().equals(referencedObject));
        return rv;
      }
    };

    // make sure that the filter filters out the sub-graph starting at the reference to tmo2.
    collector.collect(testFilter, getRootIds(), new HashSet(managed.keySet()));
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertFalse(this.lookedUp.contains(tmo2.getID()));
    assertFalse(this.lookedUp.contains(tmo3.getID()));

    // try it with the regular filter to make sure the behavior is actually different.
    collector.collect(filter, getRootIds(), new HashSet(managed.keySet()));
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

    collector.collect(filter, getRootIds(), new HashSet(managed.keySet()));
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
    // System.out.println("Creating Object:" + tmo.getID());
    managed.put(tmo.getID(), tmo.getReference());
    return tmo;
  }

  public ManagedObject getObjectByID(ObjectID id) {
    this.lookedUp.add(id);
    ManagedObjectReference ref = (ManagedObjectReference) managed.get(id);
    return (ref == null) ? null : ref.getObject();
  }

  public void release(PersistenceTransaction tx, ManagedObject object) {
    this.released.add(object.getID());
    return;
  }

  public void releaseAll(PersistenceTransaction tx, Collection c) {
    return;
  }

  public void stop() {
    throw new ImplementMe();
  }

  public boolean lookupObjectsAndSubObjectsFor(ChannelID channelID, Collection ids,
                                               ObjectManagerResultsContext responseContext, int maxCount) {
    throw new ImplementMe();
  }

  public boolean lookupObjectsForCreateIfNecessary(ChannelID channelID, Collection ids,
                                                   ObjectManagerResultsContext context) {
    throw new ImplementMe();
  }

  public Iterator getRoots() {
    throw new ImplementMe();
  }

  public void createObject(ManagedObject object) {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID id) {
    throw new ImplementMe();
  }

  public ObjectID lookupRootID(String name) {
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

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }

  public void releaseReadOnly(ManagedObject object) {
    this.released.add(object.getID());
    return;
  }

  public void dump() {
    throw new ImplementMe();
  }

  public void releaseAll(Collection objects) {
    releaseAll(transactionProvider.nullTransaction(), objects);
  }

  public int getCheckedOutCount() {
    return 0;
  }

  public Set getRootIDs() {
    return roots;
  }

  public SyncObjectIdSet getAllObjectIDs() {
    SyncObjectIdSet rv = new SyncObjectIdSet();
    rv.addAll(managed.keySet());
    return rv;
  }

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
    throw new ImplementMe();
  }

  public void waitUntilReadyToGC() {
    throw new ImplementMe();
  }

  public void notifyGCComplete(Set toDelete) {
    throw new ImplementMe();
  }

  public void flushAndEvict(List objects2Flush) {
    throw new ImplementMe();
  }

}
