/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrintable;
import com.tc.util.SyncObjectIdSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * manages all access to objects on the server. This will be single threaded and only accessed via it's event handler.
 *
 * @author steve
 */
public interface ObjectManager extends ManagedObjectProvider, PrettyPrintable {

  public void stop();

  /**
   * release object so that if anyone needs it they can have it
   *
   * @param object
   */
  public void release(PersistenceTransaction tx, ManagedObject object);

  /**
   * release all objects
   */
  public void releaseAll(Collection objects);

  /**
   * release for objects that can not have changed while checked out
   */
  public void releaseReadOnly(ManagedObject object);

  /**
   * Release all objects in the given collection.
   *
   * @param collection
   */
  public void releaseAll(PersistenceTransaction tx, Collection collection);

  /**
   * Looks up the objects associated with the Object Lookups from the clients. What it does is if all the objects are
   * available it calls setResult() o ObjectManagerResultsContext. If not then it calls makesPending on
   * ObjectManagerResultsContext and hangs on to the request until it can be fullfilled.
   *
   * @param channelID - channelID of the client that is interested in lookup
   * @param ids - ObjectIDs to be lookedup
   * @param context - ResultContext that gets notifications.
   * @param maxCount - max number of objects reachable from the requested objects that should be looked up
   * @return true if all the objects are successfully looked up.
   */
  public boolean lookupObjectsAndSubObjectsFor(ChannelID channelID, Collection ids,
                                            ObjectManagerResultsContext responseContext, int maxCount);

  /**
   * Looks up the objects associated with the transaction. What it does is if all the objects are available to be
   * updated it calls setResult() on ObjectManagerResultsContext. If not then it calls makesPending on
   * ObjectManagerResultsContext and hangs on to the request until it can be fullfilled.
   *
   * @param channelID - channelID of the client that is interested in lookup
   * @param ids - ObjectIDs to be lookedup
   * @param context - ResultContext that gets notifications.
   * @return true if all the objects are successfully looked up.
   */
  public boolean lookupObjectsForCreateIfNecessary(ChannelID channelID, Collection ids,
                                                   ObjectManagerResultsContext context);

  /**
   * The list of rootnames
   *
   * @return
   */
  public Iterator getRoots();

  public void createRoot(String name, ObjectID id);

  public ObjectID lookupRootID(String name);

  public void setGarbageCollector(GarbageCollector gc);

  /**
   * Called by GC thread (in object manager)
   */
  public void waitUntilReadyToGC();

  /**
   * Called by GC thread (in object manager)
   * @param toDelete
   */
  public void notifyGCComplete(Set toDelete);

  public void setStatsListener(ObjectManagerStatsListener listener);

  public void start();

  public void dump();

  public int getCheckedOutCount();

  public Set getRootIDs();

  public SyncObjectIdSet getAllObjectIDs();

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease);
  
  //XXX::TODO:: This will change
  public void flushAndEvict(List objects2Flush);

}
