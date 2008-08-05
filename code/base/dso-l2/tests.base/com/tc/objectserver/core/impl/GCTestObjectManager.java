/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.Evictable;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
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
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;

public class GCTestObjectManager implements ObjectManager, Evictable {

  protected Set                            roots               = new HashSet<ObjectID>();
  protected Map                            managed             = new LinkedHashMap<ObjectID, ManagedObjectReference>();
  protected Map                            swappedToDisk       = new HashMap<ObjectID, ManagedObjectReference>();
  protected GarbageCollector               gcCollector;
  protected Set                            gced                = new HashSet<ObjectID>();

  protected Set                            lookedUp            = null;
  protected Set                            released            = null;
  protected PersistenceTransactionProvider transactionProvider = null;
  
  public GCTestObjectManager(Set lookedUp, Set released, PersistenceTransactionProvider transactionProvider) {
    this.lookedUp = lookedUp;
    this.released = released;
    this.transactionProvider = transactionProvider;
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
    releaseAll(transactionProvider.nullTransaction(), objects);
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

  public void notifyGCComplete(GCResultContext resultContext) {

    
    GarbageCollectionInfo gcInfo = resultContext.getGCInfo();
    GarbageCollectionInfoPublisher gcPublisher = resultContext.getGCPublisher();
    
     
    gcPublisher.fireGCDeleteEvent(gcInfo);
    long start = System.currentTimeMillis();
   

    SortedSet<ObjectID> ids = resultContext.getGCedObjectIDs();
    for (Iterator i = ids.iterator(); i.hasNext();) {
      ObjectID objectID = (ObjectID) i.next();
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
    if(managed.containsKey(id) ) {
      return getObjectByIDOrNull(id);
    } else {
      return null;
    }
  }
}