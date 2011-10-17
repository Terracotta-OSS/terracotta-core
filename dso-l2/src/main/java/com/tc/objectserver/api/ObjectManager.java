/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages all access to objects on the server. This will be single threaded and only accessed via it's event handler.
 */
public interface ObjectManager extends ManagedObjectProvider, ObjectManagerMBean {

  public void stop();

  /**
   * releases the object and commits the transaction, so that if anyone needs it they can have it
   */
  public void releaseAndCommit(PersistenceTransaction tx, ManagedObject object);

  /**
   * release all objects
   */
  public void releaseAllReadOnly(Collection<ManagedObject> objects);

  /**
   * release for objects that can not have changed while checked out
   */
  public void releaseReadOnly(ManagedObject object);

  /**
   * Release all objects in the given collection and commits the transaction too.
   * 
   * @param collection
   */
  public void releaseAllAndCommit(PersistenceTransaction tx, Collection<ManagedObject> collection);

  /**
   * Looks up the objects associated with the Object Lookups from the clients. What it does is if all the objects are
   * available it calls setResult() o ObjectManagerResultsContext. If not then it calls makesPending on
   * ObjectManagerResultsContext and hangs on to the request until it can be fulfilled.
   * 
   * @param nodeID - nodeID of the client that is interested in lookup
   * @param maxCount - max number of objects reachable from the requested objects that should be looked up
   * @param responseContext - ResultContext that gets notifications.
   * @return true if all the objects are successfully looked up.
   */
  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext, int maxCount);

  /**
   * Looks up the objects associated with the transaction. What it does is if all the objects are available to be
   * updated it calls setResult() on ObjectManagerResultsContext. If not then it calls makesPending on
   * ObjectManagerResultsContext and hangs on to the request until it can be fulfilled.
   * 
   * @param nodeID - nodeID of the client that is interested in lookup
   * @param context - ResultContext that gets notifications.
   * @return true if all the objects are successfully looked up.
   */
  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context);

  /**
   * The list of root names
   * 
   * @return
   */
  public Iterator getRoots();

  public Map getRootNamesToIDsMap();

  public void createRoot(String name, ObjectID id);

  public void createNewObjects(Set<ObjectID> ids);

  public ObjectID lookupRootID(String name);

  public GarbageCollector getGarbageCollector();

  public void setGarbageCollector(GarbageCollector gc);

  /**
   * This method return a set of ids that are the children of the param
   * 
   * @param id - to return children of
   * @param cacheOnly - return set if only in cache.
   */
  public Set<ObjectID> getObjectReferencesFrom(ObjectID id, boolean cacheOnly);

  /**
   * Called by DGC thread (in object manager)
   */
  public void waitUntilReadyToGC();

  public int getLiveObjectCount();

  /**
   * Called by DGC thread (in object manager)
   * 
   * @param periodicDGCResultContext
   */
  public void notifyGCComplete(DGCResultContext dgcResultContext);

  public void setStatsListener(ObjectManagerStatsListener listener);

  public void start();

  public int getCheckedOutCount();

  public Set getRootIDs();

  public ObjectIDSet getAllObjectIDs();

  public ObjectIDSet getObjectIDsInCache();

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease);

  public void flushAndEvict(List objects2Flush);

  public void preFetchObjectsAndCreate(Set<ObjectID> oids, Set<ObjectID> newOids);

  public ManagedObject getObjectByIDOrNull(ObjectID id);

  /**
   * This method does not update the cache hit/miss stats. You may want to use this if you have prefetched the objects.
   */
  public ManagedObject getQuietObjectByID(ObjectID id);

}
