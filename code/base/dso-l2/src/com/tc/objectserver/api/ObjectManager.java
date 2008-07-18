/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * manages all access to objects on the server. This will be single threaded and only accessed via it's event handler.
 * 
 */
public interface ObjectManager extends ManagedObjectProvider {

  public void stop();

  /**
   * releases the object and commits the transaction, so that if anyone needs it they can have it
   * 
   * @param object
   */
  public void release(PersistenceTransaction tx, ManagedObject object);

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
  public void releaseAll(PersistenceTransaction tx, Collection<ManagedObject> collection);

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
   * Called by GC thread (in object manager)
   */
  public void waitUntilReadyToGC();

  /**
   * Called by GC thread (in object manager)
   * 
   * @param resultContext
   */
  public void notifyGCComplete(GCResultContext resultContext);
 
  public void setStatsListener(ObjectManagerStatsListener listener);

  public void start();

  public int getCheckedOutCount();

  public Set getRootIDs();

  public ObjectIDSet getAllObjectIDs();

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease);

  public void flushAndEvict(List objects2Flush);

  public void preFetchObjectsAndCreate(Set<ObjectID> oids, Set<ObjectID> newOids);

  /**
   * This method returns null if you are looking up a newly created object that is not yet initialized. This is mainly
   * used by DGC.
   */
  public ManagedObject getObjectByIDOrNull(ObjectID id);
}
