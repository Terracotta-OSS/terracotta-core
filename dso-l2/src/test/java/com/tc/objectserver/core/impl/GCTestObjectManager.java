/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.Evictable;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

public class GCTestObjectManager implements ObjectManager, Evictable {

  protected Set                            roots               = new HashSet<ObjectID>();
  protected Map                            managed             = new LinkedHashMap<ObjectID, ManagedObjectReference>();
  protected Map                            swappedToDisk       = new HashMap<ObjectID, ManagedObjectReference>();
  protected GarbageCollector               gcCollector;
  protected Set                            gced                = new HashSet<ObjectID>();

  protected Set                            lookedUp            = null;
  protected Set                            released            = null;
  protected PersistenceTransactionProvider transactionProvider = null;
  protected GarbageCollectionInfoPublisher gcPublisher;

  public GCTestObjectManager(Set lookedUp, Set released, PersistenceTransactionProvider transactionProvider) {
    this.lookedUp = lookedUp;
    this.released = released;
    this.transactionProvider = transactionProvider;
  }

  public void setPublisher(GarbageCollectionInfoPublisher gcPublisher) {
    this.gcPublisher = gcPublisher;
  }

  public ManagedObject getObjectByID(ObjectID id) {
    lookedUp.add(id);
    ManagedObjectReference ref = (ManagedObjectReference) managed.get(id);
    if (ref == null) ref = (ManagedObjectReference) swappedToDisk.get(id);
    return (ref == null) ? null : ref.getObject();
  }

  public Set getGCedObjectIDs() {
    HashSet hs = new HashSet(gced);
    gced.clear();
    return hs;
  }

  public void releaseAndCommit(PersistenceTransaction tx, ManagedObject object) {
    released.add(object.getID());
    return;
  }

  public void releaseAllAndCommit(PersistenceTransaction tx, Collection c) {
    return;
  }

  public void stop() {
    throw new ImplementMe();
  }

  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext, int maxCount) {
    throw new ImplementMe();
  }

  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
    throw new ImplementMe();
  }

  public Iterator getRoots() {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID id) {
    roots.add(id);
  }

  public ObjectID lookupRootID(String name) {
    throw new ImplementMe();
  }

  public GarbageCollector getGarbageCollector() {
    return this.gcCollector;
  }

  public void setGarbageCollector(GarbageCollector gc) {
    this.gcCollector = gc;
  }

  public void setStatsListener(ObjectManagerStatsListener listener) {
    throw new ImplementMe();
  }

  public void start() {
    this.gcCollector.start();
  }

  public void releaseReadOnly(ManagedObject object) {
    released.add(object.getID());
    return;
  }

  public void releaseAllReadOnly(Collection objects) {
    releaseAllAndCommit(transactionProvider.newTransaction(), objects);
  }

  public int getCheckedOutCount() {
    return 0;
  }

  public ObjectIDSet getAllObjectIDs() {
    ObjectIDSet oids = new ObjectIDSet(managed.keySet());
    oids.addAll(swappedToDisk.keySet());
    return oids;
  }

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
    throw new ImplementMe();
  }

  public void waitUntilReadyToGC() {
    gcCollector.notifyReadyToGC();
  }

  public Set getRootIDs() {
    return new HashSet(roots);
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

  public void createObject(ObjectID id, ManagedObjectReference mor) {
    managed.put(id, mor);
    gcCollector.notifyObjectCreated(id);
    gcCollector.notifyNewObjectInitalized(id);
  }

  public ManagedObject getObjectByIDOrNull(ObjectID id) {
    ManagedObject mo = getObjectByID(id);
    if (mo != null && mo.isNew()) { return null; }
    return mo;
  }

  // TODO: just garbage collector complete interface.
  public void notifyGCComplete(DGCResultContext dgcResultContext) {
    GarbageCollectionInfo gcInfo = dgcResultContext.getGCInfo();

    gcPublisher.fireGCDeleteEvent(gcInfo);
    long start = System.currentTimeMillis();

    SortedSet<ObjectID> ids = dgcResultContext.getGarbageIDs();
    for (Object element : ids) {
      ObjectID objectID = (ObjectID) element;
      managed.remove(objectID);
      swappedToDisk.remove(objectID);
    }
    int b4 = gced.size();
    gced.addAll(ids);
    Assert.assertEquals(b4 + ids.size(), gced.size());

    long elapsed = System.currentTimeMillis() - start;
    gcInfo.setDeleteStageTime(elapsed);
    long endMillis = System.currentTimeMillis();
    gcInfo.setElapsedTime(endMillis - gcInfo.getStartTime());
    gcInfo.setEndObjectCount(managed.size());

    gcPublisher.fireGCCompletedEvent(gcInfo);
  }

  public ObjectIDSet getObjectIDsInCache() {
    return new ObjectIDSet(managed.keySet());
  }

  public void evictCache(CacheStats stat) {
    int count = stat.getObjectCountToEvict(managed.size());
    Iterator i = managed.entrySet().iterator();
    List evicted = new ArrayList();
    while (count-- > 0 && i.hasNext()) {
      Map.Entry e = (Entry) i.next();
      ManagedObjectReference mor = (ManagedObjectReference) e.getValue();
      swappedToDisk.put(e.getKey(), mor);
      i.remove();
      evicted.add(mor.getObject());
    }
    gcCollector.notifyObjectsEvicted(evicted);
  }

  public void evict(ObjectID id) {
    ManagedObjectReference swapped = (ManagedObjectReference) managed.remove(id);
    swappedToDisk.put(id, swapped);
    ArrayList evicted = new ArrayList();
    evicted.add(swapped.getObject());
    gcCollector.notifyObjectsEvicted(evicted);
  }

  public ManagedObject getObjectFromCacheByIDOrNull(ObjectID id) {
    if (managed.containsKey(id)) {
      return getObjectByIDOrNull(id);
    } else {
      return null;
    }
  }

  public ObjectIDSet getObjectReferencesFrom(ObjectID id, boolean cacheOnly) {
    if (cacheOnly) {
      ManagedObject obj = getObjectFromCacheByIDOrNull(id);
      if (obj == null) {
        // Not in cache, rescue stage to take care of these inward references.
        return new ObjectIDSet();
      }
      Set refs = obj.getObjectReferences();
      releaseReadOnly(obj);
      return new ObjectIDSet(refs);
    } else {
      ManagedObject obj = getObjectByIDOrNull(id);
      if (obj == null) { return new ObjectIDSet(); }
      Set refs = obj.getObjectReferences();
      releaseReadOnly(obj);
      return new ObjectIDSet(refs);
    }
  }

  public String getObjectTypeFromID(ObjectID id, boolean cacheOnly) {
    return null;
  }

  public int getLiveObjectCount() {
    return managed.size();
  }

  public int getCachedObjectCount() {
    return 0;
  }

  public Iterator getRootNames() {
    return null;
  }

  public ManagedObjectFacade lookupFacade(ObjectID id, int limit) {
    return null;
  }

  public ManagedObject getQuietObjectByID(ObjectID id) {
    return getObjectByID(id);
  }

  public void scheduleGarbageCollection(GCType type, long delay) {
    throw new ImplementMe();
  }

}