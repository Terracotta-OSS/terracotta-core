/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerEventListener;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestObjectManager implements ObjectManager {

  public boolean makePending = false;

  public TestObjectManager() {
    super();
  }

  public void stop() {
    throw new ImplementMe();
  }

  public ManagedObjectFacade lookupFacade(ObjectID id) {
    throw new ImplementMe();
  }

  public boolean lookupObjectsAndSubObjectsFor(ChannelID channelID, Collection ids,
                                               ObjectManagerResultsContext context, int maxCount) {
    return basicLookup(channelID, ids, context, maxCount);
  }

  public LinkedQueue lookupObjectForCreateIfNecessaryContexts = new LinkedQueue();

  public boolean lookupObjectsForCreateIfNecessary(ChannelID channelID, Collection ids,
                                                   ObjectManagerResultsContext context) {
    Object[] args = new Object[] { channelID, ids, context };
    try {
      lookupObjectForCreateIfNecessaryContexts.put(args);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
    return basicLookup(channelID, ids, context, -1);
  }

  private boolean basicLookup(ChannelID channelID, Collection ids, ObjectManagerResultsContext context, int i) {
    if (makePending) {
      context.makePending(channelID, ids);
    } else {
      context.setResults(channelID, ids, new ObjectManagerLookupResultsImpl(createLookResults(ids)));
    }
    return !makePending;
  }

  public void processPending(Object[] args) {
    basicLookup((ChannelID) args[0], (Collection) args[1], (ObjectManagerResultsContext) args[2], -1);
  }

  private Map createLookResults(Collection ids) {
    Map results = new HashMap();
    for (Iterator i = ids.iterator(); i.hasNext();) {
      ObjectID id = (ObjectID) i.next();
      TestManagedObject tmo = new TestManagedObject(id);
      results.put(id, tmo);
    }
    return results;
  }

  public Iterator getRoots() {
    throw new ImplementMe();
  }

  public void createObject(ManagedObject object) {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID id) {
    //
  }

  public ObjectID lookupRootID(String name) {
    throw new ImplementMe();
  }

  public void setGarbageCollector(GarbageCollector gc) {
    throw new ImplementMe();
  }

  public void addListener(ObjectManagerEventListener listener) {
    throw new ImplementMe();
  }

  public ManagedObject getObjectByID(ObjectID id) {
    throw new ImplementMe();
  }

  public final LinkedQueue releaseContextQueue = new LinkedQueue();

  public void release(PersistenceTransaction tx, ManagedObject object) {
    try {
      releaseContextQueue.put(object);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void releaseReadOnly(ManagedObject object) {
    try {
      releaseContextQueue.put(object);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public final LinkedQueue releaseAllQueue = new LinkedQueue();

  public void releaseAll(PersistenceTransaction tx, Collection collection) {
    try {
      releaseAllQueue.put(collection);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }

  public final NoExceptionLinkedQueue startCalls = new NoExceptionLinkedQueue();

  public void start() {
    startCalls.put(new Object());
  }

  public void setStatsListener(ObjectManagerStatsListener listener) {
    throw new ImplementMe();
  }

  public void dump() {
    throw new ImplementMe();
  }

  public void releaseAll(Collection objects) {
    releaseAll(null, objects);
  }

  public int getCheckedOutCount() {
    return 0;
  }

  public Set getRootIDs() {
    return new HashSet();
  }

  public Set getAllObjectIDs() {
    return new HashSet();
  }

  public Object getLock() {
    return this;
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
