/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TestObjectManager implements ObjectManager, ObjectStatsManager {

  public boolean makePending = false;

  public TestObjectManager() {
    super();
  }

  @Override
  public void stop() {
    throw new ImplementMe();
  }

  public ManagedObjectFacade lookupFacade(ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext context, int maxCount) {
    return basicLookup(nodeID, context, maxCount);
  }

  public LinkedQueue lookupObjectForCreateIfNecessaryContexts = new LinkedQueue();

  @Override
  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
    Object[] args = new Object[] { nodeID, context };
    try {
      lookupObjectForCreateIfNecessaryContexts.put(args);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
    return basicLookup(nodeID, context, -1);
  }

  private boolean basicLookup(NodeID nodeID, ObjectManagerResultsContext context, int i) {
    if (!makePending) {
      context.setResults(new ObjectManagerLookupResultsImpl(createLookResults(context.getLookupIDs()),
                                                            TCCollections.EMPTY_OBJECT_ID_SET,
                                                            TCCollections.EMPTY_OBJECT_ID_SET));
    }
    return !makePending;
  }

  public void processPending(Object[] args) {
    basicLookup((NodeID) args[0], (ObjectManagerResultsContext) args[1], -1);
  }

  private Map<ObjectID, ManagedObject> createLookResults(Collection<ObjectID> ids) {
    Map<ObjectID, ManagedObject> results = new HashMap<ObjectID, ManagedObject>();
    for (final ObjectID id : ids) {
      TestManagedObject tmo = new TestManagedObject(id);
      results.put(id, tmo);
    }
    return results;
  }

  @Override
  public Iterator getRoots() {
    throw new ImplementMe();
  }

  public void createObject(ManagedObject object) {
    throw new ImplementMe();
  }

  @Override
  public void createRoot(String name, ObjectID id) {
    //
  }

  @Override
  public ObjectID lookupRootID(String name) {
    throw new ImplementMe();
  }

  @Override
  public void setGarbageCollector(GarbageCollector gc) {
    throw new ImplementMe();
  }

  public void addListener(GCStatsEventListener listener) {
    throw new ImplementMe();
  }

  @Override
  public ManagedObject getObjectByID(ObjectID id) {
    throw new ImplementMe();
  }

  public final LinkedQueue releaseContextQueue = new LinkedQueue();

  @Override
  public void release(ManagedObject object) {
    try {
      releaseContextQueue.put(object);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public void releaseReadOnly(ManagedObject object) {
    try {
      releaseContextQueue.put(object);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public final LinkedQueue releaseAllQueue = new LinkedQueue();

  @Override
  public void releaseAll(Collection collection) {
    try {
      releaseAllQueue.put(collection);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public PrettyPrinterImpl prettyPrint(PrettyPrinterImpl out) {
    throw new ImplementMe();
  }

  public final NoExceptionLinkedQueue startCalls = new NoExceptionLinkedQueue();

  @Override
  public void start() {
    startCalls.put(new Object());
  }

  @Override
  public void setStatsListener(ObjectManagerStatsListener listener) {
    throw new ImplementMe();
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
  public Set getRootIDs() {
    return new HashSet();
  }

  @Override
  public ObjectIDSet getAllObjectIDs() {
    return new ObjectIDSet();
  }

  public Object getLock() {
    return this;
  }

  @Override
  public void waitUntilReadyToGC() {
    throw new ImplementMe();
  }

  @Override
  public void notifyGCComplete(DGCResultContext dgcResultContext) {
    throw new ImplementMe();
  }

  @Override
  public void deleteObjects(Set<ObjectID> objectsToDelete) {
    throw new ImplementMe();
  }

  @Override
  public Map getRootNamesToIDsMap() {
    throw new ImplementMe();
  }

  @Override
  public GarbageCollector getGarbageCollector() {
    throw new ImplementMe();
  }

  public String dump() {
    throw new ImplementMe();
  }

  public void dump(Writer writer) {
    throw new ImplementMe();

  }

  @Override
  public void preFetchObjectsAndCreate(Set oids, Set newOids) {
    // Nop
  }

  @Override
  public void createNewObjects(Set ids) {
    throw new ImplementMe();
  }

  @Override
  public ManagedObject getObjectByIDReadOnly(ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public ObjectIDSet getObjectIDsInCache() {
    throw new ImplementMe();
  }

  public ManagedObject getObjectFromCacheByIDOrNull(ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public ObjectIDSet getObjectReferencesFrom(ObjectID id, boolean cacheOnly) {
    throw new ImplementMe();
  }

  @Override
  public String getObjectTypeFromID(ObjectID id) {
    return "";
  }

  @Override
  public int getLiveObjectCount() {
    return 0;
  }

  public int getCachedObjectCount() {
    return 0;
  }

  @Override
  public Iterator getRootNames() {
    return null;
  }

  @Override
  public ManagedObjectFacade lookupFacade(ObjectID id, int limit) {
    return null;
  }

  @Override
  public ManagedObject getQuietObjectByID(ObjectID id) {
    return getObjectByID(id);
  }

  public void scheduleGarbageCollection(GCType type, long delay) {
    throw new ImplementMe();
  }
}
