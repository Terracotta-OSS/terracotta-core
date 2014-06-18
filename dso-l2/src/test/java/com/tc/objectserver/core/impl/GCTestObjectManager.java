/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.TransactionProvider;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class GCTestObjectManager implements ObjectManager {

  protected Set<ObjectID> roots = new HashSet<ObjectID>();
  protected Map<ObjectID, ManagedObjectReference> managed = new LinkedHashMap<ObjectID, ManagedObjectReference>();
  protected Map<ObjectID, ManagedObjectReference> swappedToDisk = new HashMap<ObjectID, ManagedObjectReference>();
  protected GarbageCollector gcCollector;
  protected Set<ObjectID> gced = new HashSet<ObjectID>();

  protected Set<ObjectID> lookedUp = null;
  protected Set<ObjectID> released = null;
  protected TransactionProvider transactionProvider = null;
  protected GarbageCollectionInfoPublisher gcPublisher;

  public GCTestObjectManager(Set<ObjectID> lookedUp, Set<ObjectID> released, TransactionProvider transactionProvider) {
    this.lookedUp = lookedUp;
    this.released = released;
    this.transactionProvider = transactionProvider;
  }

  public void setPublisher(GarbageCollectionInfoPublisher gcPublisher) {
    this.gcPublisher = gcPublisher;
  }

  @Override
  public ManagedObject getObjectByID(ObjectID id) {
    lookedUp.add(id);
    ManagedObjectReference ref = managed.get(id);
    if (ref == null) ref = swappedToDisk.get(id);
    return (ref == null) ? null : ref.getObject();
  }

  public Set getGCedObjectIDs() {
    HashSet hs = new HashSet<ObjectID>(gced);
    gced.clear();
    return hs;
  }

  @Override
  public void release(ManagedObject object) {
    released.add(object.getID());
  }

  @Override
  public void releaseAll(Collection c) {
    //
  }

  @Override
  public void stop() {
    throw new ImplementMe();
  }

  @Override
  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext, int maxCount) {
    throw new ImplementMe();
  }

  @Override
  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
    throw new ImplementMe();
  }

  @Override
  public Iterator getRoots() {
    throw new ImplementMe();
  }

  @Override
  public void createRoot(String name, ObjectID id) {
    roots.add(id);
  }

  @Override
  public ObjectID lookupRootID(String name) {
    throw new ImplementMe();
  }

  @Override
  public GarbageCollector getGarbageCollector() {
    return this.gcCollector;
  }

  @Override
  public void setGarbageCollector(GarbageCollector gc) {
    this.gcCollector = gc;
  }

  @Override
  public void start() {
    this.gcCollector.start();
  }

  @Override
  public void releaseReadOnly(ManagedObject object) {
    released.add(object.getID());
  }

  @Override
  public void releaseAllReadOnly(Collection objects) {
    releaseAll(objects);
  }

  @Override
  public int getCheckedOutCount() {
    return 0;
  }

  @Override
  public ObjectIDSet getAllObjectIDs() {
    ObjectIDSet oids = new BitSetObjectIDSet(managed.keySet());
    oids.addAll(swappedToDisk.keySet());
    return oids;
  }

  @Override
  public void waitUntilReadyToGC() {
    gcCollector.notifyReadyToGC();
  }

  @Override
  public Set getRootIDs() {
    return new HashSet<ObjectID>(roots);
  }

  @Override
  public Map getRootNamesToIDsMap() {
    throw new ImplementMe();
  }

  @Override
  public void createNewObjects(Set ids) {
    throw new ImplementMe();
  }

  public void createObject(ObjectID id, ManagedObjectReference mor) {
    managed.put(id, mor);
  }

  @Override
  public ManagedObject getObjectByIDReadOnly(ObjectID id) {
    ManagedObject mo = getObjectByID(id);
    if (mo != null && mo.isNew()) {
      return null;
    }
    return mo;
  }

  @Override
  public void notifyGCComplete(DGCResultContext dgcResultContext) {
    SortedSet<ObjectID> ids = dgcResultContext.getGarbageIDs();
    for (Object element : ids) {
      ObjectID objectID = (ObjectID) element;
      managed.remove(objectID);
      swappedToDisk.remove(objectID);
    }
    int b4 = gced.size();
    gced.addAll(ids);
    Assert.assertEquals(b4 + ids.size(), gced.size());
  }

  @Override
  public ObjectIDSet getObjectIDsInCache() {
    return new BitSetObjectIDSet(managed.keySet());
  }

  public ManagedObject getObjectFromCacheByIDOrNull(ObjectID id) {
    if (managed.containsKey(id)) {
      return getObjectByIDReadOnly(id);
    } else {
      return null;
    }
  }

  @Override
  public ObjectIDSet getObjectReferencesFrom(ObjectID id, boolean cacheOnly) {
    if (cacheOnly) {
      ManagedObject obj = getObjectFromCacheByIDOrNull(id);
      if (obj == null) {
        // Not in cache, rescue stage to take care of these inward references.
        return new BitSetObjectIDSet();
      }
      Set refs = obj.getObjectReferences();
      releaseReadOnly(obj);
      return new BitSetObjectIDSet(refs);
    } else {
      ManagedObject obj = getObjectByIDReadOnly(id);
      if (obj == null) {
        return new BitSetObjectIDSet();
      }
      Set refs = obj.getObjectReferences();
      releaseReadOnly(obj);
      return new BitSetObjectIDSet(refs);
    }
  }

  @Override
  public int getLiveObjectCount() {
    return managed.size();
  }

  @Override
  public Iterator getRootNames() {
    return null;
  }

  @Override
  public Set<ObjectID> deleteObjects(final Set<ObjectID> objectsToDelete) {
    return Collections.EMPTY_SET;
  }

  @Override
  public Set<ObjectID> tryDeleteObjects(final Set<ObjectID> objectsToDelete, final Set<ObjectID> checkedOutObjects) {
    return Collections.EMPTY_SET;
  }
}