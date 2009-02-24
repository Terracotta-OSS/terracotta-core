/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.logging.DumpHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.Evictable;
import com.tc.object.cache.EvictionPolicy;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.context.ManagedObjectFlushingContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.NullGarbageCollector;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.NullTransactionalObjectManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;
import com.tc.util.Counter;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages access to all the Managed objects in the system.
 */
public class ObjectManagerImpl implements ObjectManager, ManagedObjectChangeListener, ObjectManagerMBean, Evictable,
    DumpHandler, PrettyPrintable {

  private static final TCLogger                       logger                   = TCLogging
                                                                                   .getLogger(ObjectManager.class);

  private static final int                            MAX_COMMIT_SIZE          = TCPropertiesImpl
                                                                                   .getProperties()
                                                                                   .getInt(
                                                                                           TCPropertiesConsts.L2_OBJECTMANAGER_MAXOBJECTS_TO_COMMIT);
  // XXX:: Should go to property file
  private static final int                            INITIAL_SET_SIZE         = 16;
  private static final float                          LOAD_FACTOR              = 0.75f;
  private static final int                            MAX_LOOKUP_OBJECTS_COUNT = 5000;

  private final ManagedObjectStore                    objectStore;
  private final Map<ObjectID, ManagedObjectReference> references;
  private final EvictionPolicy                        evictionPolicy;
  private final Counter                               flushCount               = new Counter();
  private final PendingList                           pending                  = new PendingList();

  private GarbageCollector                            collector                = new NullGarbageCollector();
  private int                                         checkedOutCount          = 0;

  private volatile boolean                            inShutdown               = false;

  private final ClientStateManager                    stateManager;
  private final ObjectManagerConfig                   config;
  private ObjectManagerStatsListener                  stats                    = new NullObjectManagerStatsListener();
  private final PersistenceTransactionProvider        persistenceTransactionProvider;
  private final Sink                                  faultSink;
  private final Sink                                  flushSink;
  private TransactionalObjectManager                  txnObjectMgr             = new NullTransactionalObjectManager();
  private int                                         preFetchedCount          = 0;

  private final ObjectStatsRecorder                   objectStatsRecorder;

  public ObjectManagerImpl(ObjectManagerConfig config, ClientStateManager stateManager, ManagedObjectStore objectStore,
                           EvictionPolicy cache, PersistenceTransactionProvider persistenceTransactionProvider,
                           Sink faultSink, Sink flushSink, ObjectStatsRecorder objectStatsRecorder) {
    this.faultSink = faultSink;
    this.flushSink = flushSink;
    Assert.assertNotNull(objectStore);
    this.config = config;
    this.stateManager = stateManager;
    this.objectStore = objectStore;
    this.evictionPolicy = cache;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.references = new HashMap<ObjectID, ManagedObjectReference>(10000);
    this.objectStatsRecorder = objectStatsRecorder;
  }

  public void setTransactionalObjectManager(TransactionalObjectManager txnObjectManager) {
    this.txnObjectMgr = txnObjectManager;
  }

  public void setStatsListener(ObjectManagerStatsListener statsListener) {
    this.stats = statsListener;
  }

  public void start() {
    this.collector.start();
  }

  public synchronized void stop() {
    this.inShutdown = true;

    this.collector.stop();

    // flush the cache to stable persistence.
    Set<ManagedObject> toFlush = new HashSet<ManagedObject>();
    for (final ManagedObjectReference ref : this.references.values()) {
      ManagedObject obj = ref.getObject();
      if (!obj.isNew()) {
        toFlush.add(obj);
      }
    }
    PersistenceTransaction tx = newTransaction();
    flushAllAndCommit(tx, toFlush);
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().print("collector: ").visit(this.collector).println();
    out.indent().print("references: ").visit(this.references).println();

    out.indent().println("checkedOutCount: " + this.checkedOutCount);
    out.indent().print("pending: ").visit(this.pending).println();

    out.indent().print("objectStore: ").duplicateAndIndent().visit(this.objectStore).println();
    out.indent().print("stateManager: ").duplicateAndIndent().visit(this.stateManager).println();
    try {

      StringBuffer rootBuff = new StringBuffer();
      for (Iterator rootIter = getRootNames(); rootIter.hasNext();) {
        rootBuff.append(rootIter.next());
        if (rootIter.hasNext()) {
          rootBuff.append(",");
        }
      }
      out.indent().print("roots: " + rootBuff.toString()).println();
    } catch (Throwable t) {
      logger.error("exception printing roots in ObjectManagerImpl", t);
    }
    return out;
  }

  public ObjectID lookupRootID(String name) {
    syncAssertNotInShutdown();
    return this.objectStore.getRootID(name);
  }

  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                               int maxReachableObjects) {
    // maxReachableObjects is at least 1 so that addReachableObjectsIfNecessary does the right thing
    return lookupObjectsForOptionallyCreate(nodeID, responseContext, maxReachableObjects <= 0 ? 1 : maxReachableObjects);
  }

  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext) {
    return lookupObjectsForOptionallyCreate(nodeID, responseContext, -1);
  }

  private synchronized boolean lookupObjectsForOptionallyCreate(NodeID nodeID,
                                                                ObjectManagerResultsContext responseContext,
                                                                int maxReachableObjects) {
    syncAssertNotInShutdown();

    if (this.collector.isPausingOrPaused()) {
      makePending(nodeID, new ObjectManagerLookupContext(responseContext, false), maxReachableObjects);
      return false;
    }
    return basicLookupObjectsFor(nodeID, new ObjectManagerLookupContext(responseContext, false), maxReachableObjects);
  }

  public Iterator getRoots() {
    syncAssertNotInShutdown();
    return this.objectStore.getRoots().iterator();
  }

  public Iterator getRootNames() {
    syncAssertNotInShutdown();
    return this.objectStore.getRootNames().iterator();
  }

  /**
   * For management use only (see interface documentation)
   */
  public ManagedObjectFacade lookupFacade(ObjectID id, int limit) throws NoSuchObjectException {
    final ManagedObject object = lookup(id, true, false);
    if (object == null) { throw new NoSuchObjectException(id); }

    try {

      return object.createFacade(limit);
    } finally {
      releaseReadOnly(object);
    }
  }

  private ManagedObject lookup(ObjectID id, boolean missingOk, boolean lookupNewObjects) {
    syncAssertNotInShutdown();

    WaitForLookupContext waitContext = new WaitForLookupContext(id, missingOk, lookupNewObjects);
    ObjectManagerLookupContext context = new ObjectManagerLookupContext(waitContext, true);
    basicLookupObjectsFor(ClientID.NULL_ID, context, -1);

    ManagedObject mo = waitContext.getLookedUpObject();
    if (mo == null) {
      Assert.assertTrue(missingOk);
    }
    return mo;
  }

  public ManagedObject getObjectByID(ObjectID id) {
    return lookup(id, false, false);
  }

  /**
   * This method lookups objects just like getObjectByID except it returns null if the object is still new.
   */
  public ManagedObject getObjectByIDOrNull(ObjectID id) {
    ManagedObject mo = lookup(id, false, true);
    if (mo.isNew()) {
      logger.warn("Returning null since looking up " + id + " which is still a new Object : " + mo);
      releaseReadOnly(mo);
      return null;
    }
    return mo;
  }

  /**
   * This method returns null if you are looking up a newly created object that is not yet initialized or an Object that
   * is not in cache. This is mainly used by DGC.
   */
  public ManagedObject getObjectFromCacheByIDOrNull(ObjectID id) {
    if (isObjectInCache(id)) {
      // There is still a small race where this call might fault objects that were just flushed to disk, but we can live
      // with that.
      return getObjectByIDOrNull(id);
    } else {
      // Not in cache.
      return null;
    }
  }

  private synchronized boolean isObjectInCache(ObjectID id) {
    return this.references.containsKey(id);
  }

  private void markReferenced(ManagedObjectReference reference) {
    if (reference.isReferenced()) { throw new AssertionError("Attempt to mark an already referenced object: "
                                                             + reference); }
    reference.markReference();
    this.checkedOutCount++;
  }

  private void unmarkReferenced(ManagedObjectReference reference) {
    if (!reference.isReferenced()) { throw new AssertionError("Attempt to unmark an unreferenced object: " + reference); }
    reference.unmarkReference();
    this.checkedOutCount--;
  }

  /**
   * Retrieves materialized references.
   */
  private ManagedObjectReference getReference(ObjectID id) {
    return this.references.get(id);
  }

  /**
   * Retrieves materialized references-- if not materialized, will initiate a request to materialize them from the
   * object store.
   */
  private ManagedObjectReference getOrLookupReference(ObjectManagerLookupContext context, ObjectID id) {
    ManagedObjectReference rv = getReference(id);

    if (rv == null) {
      rv = initiateFaultingFor(id, context.removeOnRelease());
    } else if (rv instanceof FaultingManagedObjectReference) {
      // Check to see if the retrieve was complete and the Object is missing
      FaultingManagedObjectReference fmr = (FaultingManagedObjectReference) rv;
      if (!fmr.isFaultingInProgress()) {
        this.references.remove(id);
        logger.warn("Request for non-existent object : " + id + " context = " + context);
        context.missingObject(id);
        return null;
      }
      if (context.updateStats()) {
        this.stats.cacheMiss();
      }
    } else {
      if (context.updateStats()) {
        this.stats.cacheHit();
      }
      if (!context.removeOnRelease()) {
        if (rv.isRemoveOnRelease()) {
          // This Object is faulted in by GC or Management interface with removeOnRelease = true, but before they got a
          // chance to grab it, a regular request for object is received. Take corrective action.
          rv.setRemoveOnRelease(false);
          this.evictionPolicy.add(rv);
        } else {
          this.evictionPolicy.markReferenced(rv);
        }
      }
    }
    return rv;
  }

  private ManagedObjectReference initiateFaultingFor(ObjectID id, boolean removeOnRelease) {
    // Request Faulting in a different stage and give back a "Referenced" proxy
    ManagedObjectFaultingContext mofc = new ManagedObjectFaultingContext(id, removeOnRelease);
    this.faultSink.add(mofc);

    // don't account for a cache "miss" unless this was a real request
    // originating from a client
    this.stats.cacheMiss();
    return addNewReference(new FaultingManagedObjectReference(id));
  }

  public synchronized void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
    if (mo == null) {
      FaultingManagedObjectReference fmor;
      ManagedObjectReference mor = this.references.get(oid);
      if (mor == null || !(mor instanceof FaultingManagedObjectReference) || !oid.equals(mor.getObjectID())) {
        // Format
        throw new AssertionError("ManagedObjectReference is not what was expected : " + mor + " oid : " + oid);
      }
      fmor = (FaultingManagedObjectReference) mor;
      fmor.faultingFailed();
    } else {
      Assert.assertEquals(oid, mo.getID());
      ManagedObjectReference mor = this.references.remove(oid);
      if (mor == null || !(mor instanceof FaultingManagedObjectReference) || !oid.equals(mor.getObjectID())) {
        // Format
        throw new AssertionError("ManagedObjectReference is not what was expected : " + mor + " oid : " + oid);
      }
      addNewReference(mo, removeOnRelease);
    }
    makeUnBlocked(oid);
    postRelease();
  }

  public synchronized void preFetchObjectsAndCreate(Set<ObjectID> oids, Set<ObjectID> newOids) {
    createNewObjects(newOids);
    preFetchObjects(oids);
  }

  private void preFetchObjects(Set<ObjectID> oids) {
    for (final ObjectID id : oids) {
      ManagedObjectReference rv = getReference(id);
      if (rv == null) {
        // This object is not in the cache, initiate faulting for the object
        if (++this.preFetchedCount % 1000 == 0) {
          logger.info("Prefetched " + this.preFetchedCount + " objects");
        }
        initiateFaultingFor(id, false);
      } else {
        this.stats.cacheHit();
      }
    }
  }

  private ManagedObjectReference addNewReference(ManagedObject obj, boolean isRemoveOnRelease) throws AssertionError {
    ManagedObjectReference newReference = obj.getReference();
    newReference.setRemoveOnRelease(isRemoveOnRelease);

    return addNewReference(newReference);
  }

  private ManagedObjectReference addNewReference(ManagedObjectReference newReference) {
    Object oldRef = this.references.put(newReference.getObjectID(), newReference);
    if (oldRef != null) { throw new AssertionError("Object was not null. Reference already present : old = " + oldRef
                                                   + " : new = " + newReference); }
    Assert.assertTrue(newReference.getNext() == null && newReference.getPrevious() == null);

    if (!newReference.isRemoveOnRelease()) {
      this.evictionPolicy.add(newReference);
    }
    return newReference;
  }

  private synchronized void reapCache(Collection removalCandidates, Collection<ManagedObject> toFlush,
                                      Collection<ManagedObjectReference> removedObjects) {
    while (this.collector.isPausingOrPaused()) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        logger.error(e);
      }
    }
    for (final Object cand : removalCandidates) {
      ManagedObjectReference removalCandidate = (ManagedObjectReference) cand;
      // It is possible that before the cache evictor has a chance to mark the reference, the GC could come and remove
      // the reference, hence we check in references map again
      if (removalCandidate != null && !removalCandidate.isReferenced() && !removalCandidate.isNew()
          && this.references.containsKey(removalCandidate.getObjectID())) {
        this.evictionPolicy.remove(removalCandidate);
        if (removalCandidate.getObject().isDirty()) {
          Assert.assertFalse(this.config.paranoid());
          markReferenced(removalCandidate);
          toFlush.add(removalCandidate.getObject());
        } else {
          // paranoid mode or the object is not dirty - just remove from reference
          removedObjects.add(this.references.remove(removalCandidate.getObjectID()));
        }
      }
    }
    notifyCollectorEvictedObjects(toFlush);
    notifyCollectorEvictedObjects(removedObjects);
  }

  private void notifyCollectorEvictedObjects(Collection evicted) {
    this.collector.notifyObjectsEvicted(evicted);
  }

  private void evicted(Collection managedObjects) {
    synchronized (this) {
      this.checkedOutCount -= managedObjects.size();
      for (Iterator i = managedObjects.iterator(); i.hasNext();) {
        ManagedObject mo = (ManagedObject) i.next();
        ObjectID oid = mo.getID();
        Object o = this.references.remove(oid);
        if (o == null) {
          logger.warn("Object ID : " + mo.getID()
                      + " was mapped to null but should have been mapped to a reference of  " + mo);
        } else {
          ManagedObjectReference ref = (ManagedObjectReference) o;
          if (isBlocked(oid)) {
            ref.unmarkReference();
            addNewReference(mo, ref.isRemoveOnRelease());
            makeUnBlocked(oid);
            i.remove();
          }
        }
      }
      postRelease();
    }

  }

  private synchronized boolean basicLookupObjectsFor(NodeID nodeID, ObjectManagerLookupContext context,
                                                     int maxReachableObjects) {
    Set<ManagedObjectReference> objects = createNewSet();

    final Set<ObjectID> newObjectIDs = context.getNewObjectIDs();
    boolean available = true;
    Set<ObjectID> ids = context.getLookupIDs();
    for (final ObjectID id : ids) {
      if (context.isMissingObject(id)) {
        // If we already know the object is missing, don't initiate yet another lookup. this is to avoid repeated
        // lookups when there are 2 or more missing objects.
        continue;
      }

      // We don't check available flag before doing calling getOrLookupReference() for two reasons.
      // 1) To get the right hit/miss count and
      // 2) to Fault objects that are not available
      ManagedObjectReference reference = getOrLookupReference(context, id);
      if (reference == null) {
        continue;
      } else if (available && (reference.isReferenced() || (reference.isNew() && !newObjectIDs.contains(id)))) {
        available = false;
        // If reference isNew() and not in newObjects, someone (L1) is trying to do a lookup before the object is fully
        // created, make it pending.
        // Setting only the first referenced object to process Pending. If objects are being faulted in, then this
        // will ensure that we don't run processPending multiple times unnecessarily.
        addBlocked(nodeID, context, maxReachableObjects, id);
      }
      objects.add(reference);
    }

    if (available) {
      ObjectIDSet processLater = addReachableObjectsIfNecessary(nodeID, maxReachableObjects, objects);
      ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(processObjectsRequest(objects),
                                                                              processLater, context
                                                                                  .getMissingObjectIDs());
      context.setResults(results);
    } else {
      context.makeOldRequest();
    }
    return available;
  }

  public synchronized void createNewObjects(Set<ObjectID> newObjectIDs) {
    for (final ObjectID oid : newObjectIDs) {
      ManagedObject mo = new ManagedObjectImpl(oid);
      createObject(mo);
    }
  }

  private ObjectIDSet addReachableObjectsIfNecessary(NodeID nodeID, int maxReachableObjects,
                                                     Set<ManagedObjectReference> objects) {
    if (maxReachableObjects <= 0) { return TCCollections.EMPTY_OBJECT_ID_SET; }
    ManagedObjectTraverser traverser = new ManagedObjectTraverser(maxReachableObjects);
    Set<ManagedObjectReference> lookedUpObjects = objects;
    do {
      traverser.traverse(lookedUpObjects);
      lookedUpObjects = new HashSet<ManagedObjectReference>();
      Set<ObjectID> lookupObjectIDs = traverser.getObjectsToLookup();
      if (lookupObjectIDs.isEmpty()) {
        break;
      }
      this.stateManager.removeReferencedFrom(nodeID, lookupObjectIDs);
      for (final ObjectID id : lookupObjectIDs) {
        ManagedObjectReference newRef = getReference(id);
        // Note : Objects are looked up only if it is in the memory and not referenced
        if (newRef != null && !newRef.isReferenced() && !newRef.isNew()) {
          if (objects.add(newRef)) {
            lookedUpObjects.add(newRef);
          }
        }
      }
    } while (objects.size() < MAX_LOOKUP_OBJECTS_COUNT);
    return traverser.getPendingObjectsToLookup(lookedUpObjects);
  }

  public void releaseReadOnly(ManagedObject object) {
    if (this.config.paranoid() && !object.isNew() && object.isDirty()) { throw new AssertionError(
                                                                                                  "Object is dirty after a read-only checkout "
                                                                                                      + object); }
    synchronized (this) {
      basicReleaseReadOnly(object);
      postRelease();
    }
  }

  public void release(PersistenceTransaction persistenceTransaction, ManagedObject object) {
    if (this.config.paranoid()) {
      flushAndCommit(persistenceTransaction, object);
    }
    synchronized (this) {
      basicRelease(object);
      postRelease();
    }

  }

  public synchronized void releaseAllReadOnly(Collection<ManagedObject> objects) {
    for (final ManagedObject mo : objects) {
      if (this.config.paranoid() && !mo.isNew() && mo.isDirty()) {
        // It is possible to release new just created objects before it has a chance to get applied because of a recall
        // due to a GC. Check out ObjectManagerTest.testRecallNewObjects()
        throw new AssertionError("ObjectManager.releaseAll() called on dirty old objects : " + mo
                                 + " total objects size : " + objects.size());
      }
      basicReleaseReadOnly(mo);
    }
    postRelease();
  }

  /**
   * We used to not call txn.commit() here. But that implies that the objects are released for other lookups before it
   * is committed to disk. This is good for performance reason but imposes a problem. The clients could read an object
   * that has changes but it not committed to the disk yet and If the server crashes then transactions are resent and
   * may be re-applied in the clients when it should not have re-applied. To avoid this we now commit in-line before
   * releasing the objects. (A wise man in the company once said that the performance of broken code is zero)
   * <p>
   * TODO:: Implement a mechanism where Objects are marked pending to commit and give it out for other transactions but
   * not for client lookups.
   */
  public void releaseAll(PersistenceTransaction persistenceTransaction, Collection<ManagedObject> managedObjects) {
    if (this.config.paranoid()) {
      flushAllAndCommit(persistenceTransaction, managedObjects);
    }
    synchronized (this) {
      for (final ManagedObject managedObject : managedObjects) {
        basicRelease(managedObject);
      }
      postRelease();
    }
  }

  private void removeAllObjectsByID(Set<ObjectID> toDelete) {
    for (final ObjectID id : toDelete) {
      ManagedObjectReference ref = this.references.remove(id);
      while (ref != null && ref.isReferenced()) {
        // This is possible if the cache manager is evicting this *unreachable* object or somehow the admin console is
        // looking up this object.
        logger.warn("Reference : " + ref + " was referenced. So waiting to remove !");
        // reconcile
        this.references.put(id, ref);
        try {
          wait();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        ref = this.references.remove(id);
      }
      if (ref != null) {
        if (ref.isNew()) { throw new AssertionError("GCed Reference is still new : " + ref); }
        this.evictionPolicy.remove(ref);
      }
    }
  }

  public synchronized int getCheckedOutCount() {
    return this.checkedOutCount;
  }

  public Set getRootIDs() {
    return this.objectStore.getRoots();
  }

  public Map getRootNamesToIDsMap() {
    return this.objectStore.getRootNamesToIDsMap();
  }

  public ObjectIDSet getAllObjectIDs() {
    return this.objectStore.getAllObjectIDs();
  }

  public synchronized ObjectIDSet getObjectIDsInCache() {
    ObjectIDSet ids = new ObjectIDSet();
    ids.addAll(this.references.keySet());
    return ids;
  }

  public int getLiveObjectCount() {
    return this.objectStore.getObjectCount();
  }

  private void postRelease() {
    if (this.collector.isPausingOrPaused()) {
      checkAndNotifyGC();
    } else if (this.pending.size() > 0) {
      processPendingLookups();
    }
    notifyAll();
  }

  private void basicRelease(ManagedObject object) {
    ManagedObjectReference mor = object.getReference();
    updateNewFlagAndCreateIfNecessary(object);
    removeReferenceIfNecessary(mor);
    unmarkReferenced(mor);
    makeUnBlocked(object.getID());
  }

  private void basicReleaseReadOnly(ManagedObject object) {
    ManagedObjectReference mor = object.getReference();
    removeReferenceIfNecessary(mor);
    unmarkReferenced(mor);
    makeUnBlocked(object.getID());
  }

  private void updateNewFlagAndCreateIfNecessary(ManagedObject object) {
    if (object.isNew()) {
      this.objectStore.addNewObject(object);
      object.setIsNew(false);
      fireNewObjectinitialized(object.getID());
    }
  }

  private void fireNewObjectinitialized(ObjectID id) {
    this.collector.notifyNewObjectInitalized(id);
  }

  private void removeReferenceIfNecessary(ManagedObjectReference mor) {
    if (mor.isRemoveOnRelease()) {
      if (mor.getObject().isDirty()) {
        logger.error(mor + " is DIRTY");
        throw new AssertionError(mor + " is DIRTY");
      }
      Object removed = this.references.remove(mor.getObjectID());
      Assert.assertNotNull(removed);
    }
  }

  private void checkAndNotifyGC() {
    if (this.checkedOutCount == 0) {
      // logger.info("Notifying GC : pending = " + pending.size() + " checkedOutCount = " + checkedOutCount);
      this.collector.notifyReadyToGC();
    }
  }

  public synchronized void waitUntilReadyToGC() {
    checkAndNotifyGC();
    this.txnObjectMgr.recallAllCheckedoutObject();
    int count = 0;
    while (!this.collector.isPaused()) {
      if (count++ > 2) {
        logger.warn("Still waiting for object to be checked back in. collector state is not paused. checkout count = "
                    + this.checkedOutCount);
      }
      try {
        this.wait(10000);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  public void notifyGCComplete(GCResultContext gcResult) {
    Set<ObjectID> toDelete = gcResult.getGCedObjectIDs();
    synchronized (this) {
      removeAllObjectsByID(toDelete);
      // Process pending, since we disabled process pending while GC pause was initiate.
      processPendingLookups();
      notifyAll();
    }
    this.objectStore.removeAllObjectsByID(gcResult);
  }

  private void flushAndCommit(PersistenceTransaction persistenceTransaction, ManagedObject managedObject) {
    this.objectStore.commitObject(persistenceTransaction, managedObject);
    persistenceTransaction.commit();
  }

  private void flushAllAndCommit(PersistenceTransaction persistenceTransaction, Collection managedObjects) {
    this.objectStore.commitAllObjects(persistenceTransaction, managedObjects);
    persistenceTransaction.commit();
  }

  public void dumpToLogger() {
    logger.info(dump());
  }

  public String dump() {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    new PrettyPrinterImpl(pw).visit(this);
    writer.flush();
    return writer.toString();
  }

  // This method is for tests only
  public synchronized boolean isReferenced(ObjectID id) {
    ManagedObjectReference reference = getReference(id);
    return reference != null && reference.isReferenced();
  }

  // This method is public for testing purpose
  public synchronized void createObject(ManagedObject object) {
    syncAssertNotInShutdown();
    ObjectID oid = object.getID();
    Assert.eval(oid.toLong() != -1);
    // Not adding to the store yet since this transaction containing the new objects is not yet applied.
    // objectStore.addNewObject(object);
    addNewReference(object, false);
    this.stats.newObjectCreated();
    fireObjectCreated(oid);
  }

  private void fireObjectCreated(ObjectID id) {
    this.collector.notifyObjectCreated(id);
  }

  public ManagedObjectStore getObjectStore() {
    return this.objectStore;
  }

  public ClientStateManager getClientStateManager() {
    return this.stateManager;
  }

  public void createRoot(String rootName, ObjectID id) {
    syncAssertNotInShutdown();
    PersistenceTransaction tx = newTransaction();
    this.objectStore.addNewRoot(tx, rootName, id);
    tx.commit();
    this.stats.newObjectCreated();
    // This change needs to be notified so that new roots are not missedout
    changed(null, null, id);
  }

  private PersistenceTransaction newTransaction() {
    return this.persistenceTransactionProvider.newTransaction();
  }

  public GarbageCollector getGarbageCollector() {
    return this.collector;
  }

  public void setGarbageCollector(final GarbageCollector newCollector) {
    syncAssertNotInShutdown();
    if (this.collector != null) {
      this.collector.stop();
    }
    this.collector = newCollector;
  }

  private Map<ObjectID, ManagedObject> processObjectsRequest(Collection<ManagedObjectReference> objects) {
    Map<ObjectID, ManagedObject> results = new HashMap<ObjectID, ManagedObject>();
    for (final ManagedObjectReference mor : objects) {
      Assert.assertNotNull(mor);
      if (!mor.isReferenced()) {
        markReferenced(mor);
      }
      if (mor.getObject() == null) {
        logger.error("Object is NULL for " + mor);
        throw new AssertionError("ManagedObject is null.");
      }
      results.put(mor.getObjectID(), mor.getObject());
    }
    return results;
  }

  private void processPendingLookups() {
    List<Pending> lp = this.pending.getAndResetPendingRequests();
    for (final Pending p : lp) {
      basicLookupObjectsFor(p.getNodeID(), p.getRequestContext(), p.getMaxReachableObjects());
    }
  }

  private void addBlocked(NodeID nodeID, ObjectManagerLookupContext context, int maxReachableObjects,
                          ObjectID blockedOid) {
    this.pending.makeBlocked(blockedOid, new Pending(nodeID, context, maxReachableObjects));
    if (context.getProcessedCount() % 500 == 499) {
      logger.warn("Reached " + context.getProcessedCount() + " Pending size : " + this.pending.size()
                  + " : basic look up for : " + context + " maxReachable depth : " + maxReachableObjects);
    }
  }

  private void makeUnBlocked(ObjectID id) {
    this.pending.makeUnBlocked(id);
  }

  private boolean isBlocked(ObjectID id) {
    return this.pending.isBlocked(id);
  }

  private void makePending(NodeID nodeID, ObjectManagerLookupContext context, int maxReachableObjects) {
    this.pending.addPending(new Pending(nodeID, context, maxReachableObjects));
  }

  private void syncAssertNotInShutdown() {
    assertNotInShutdown();
  }

  private void assertNotInShutdown() {
    if (this.inShutdown) { throw new ShutdownError(); }
  }

  public void evictCache(CacheStats stat) {
    int size = references_size();
    int toEvict = stat.getObjectCountToEvict(size);
    if (toEvict <= 0) { return; }

    // This could be a costly call, so call just once
    Collection removalCandidates = this.evictionPolicy.getRemovalCandidates(toEvict);

    HashSet<ManagedObject> toFlush = new HashSet<ManagedObject>();
    ArrayList<ManagedObjectReference> removed = new ArrayList<ManagedObjectReference>();
    reapCache(removalCandidates, toFlush, removed);

    if (this.objectStatsRecorder.getFlushDebug()) {
      updateFlushStats(toFlush, removed);
    }

    int evicted = (toFlush.size() + removed.size());
    // Let GC work for us
    removed = null;
    removalCandidates = null;

    if (!toFlush.isEmpty()) {
      initateFlushRequest(toFlush);
      toFlush = null; // make GC work
      waitUntilFlushComplete();
    }

    // TODO:: Send the right objects to the cache manager
    stat.objectEvicted(evicted, references_size(), Collections.EMPTY_LIST);
  }

  private void updateFlushStats(Collection<ManagedObject> toFlush, Collection<ManagedObjectReference> removedObjects) {
    Iterator<ManagedObject> flushIter = toFlush.iterator();
    while (flushIter.hasNext()) {
      String className = flushIter.next().getManagedObjectState().getClassName();
      if (className == null) {
        className = "UNKNOWN";
      }
      this.objectStatsRecorder.updateFlushStats(className);
    }
    Iterator<ManagedObjectReference> removedIter = removedObjects.iterator();
    while (removedIter.hasNext()) {
      String className = removedIter.next().getObject().getManagedObjectState().getClassName();
      if (className == null) {
        className = "UNKNOWN";
      }
      this.objectStatsRecorder.updateFlushStats(className);
    }
  }

  private void waitUntilFlushComplete() {
    this.flushCount.waitUntil(0);
  }

  private void initateFlushRequest(Collection toFlush) {
    this.flushCount.increment(toFlush.size());
    for (Iterator i = toFlush.iterator(); i.hasNext();) {
      int count = 0;
      ManagedObjectFlushingContext mofc = new ManagedObjectFlushingContext();
      while (count < MAX_COMMIT_SIZE && i.hasNext()) {
        mofc.addObjectToFlush(i.next());
        count++;
        // i.remove();
      }
      this.flushSink.add(mofc);
    }
  }

  public void flushAndEvict(List objects2Flush) {
    PersistenceTransaction tx = newTransaction();
    int size = objects2Flush.size();
    flushAllAndCommit(tx, objects2Flush);
    evicted(objects2Flush);
    this.flushCount.decrement(size);
  }

  // XXX:: This is not synchronized and might not give us the right number. Performance over accuracy. This is to be
  // used only in evictCache method.
  private int references_size() {
    return this.references.size();
  }

  private static class ObjectManagerLookupContext implements ObjectManagerResultsContext {

    private final ObjectManagerResultsContext responseContext;
    private final boolean                     removeOnRelease;
    private final ObjectIDSet                 missing        = new ObjectIDSet();
    private int                               processedCount = 0;

    public ObjectManagerLookupContext(ObjectManagerResultsContext responseContext, boolean removeOnRelease) {
      this.responseContext = responseContext;
      this.removeOnRelease = removeOnRelease;
    }

    public boolean isMissingObject(ObjectID id) {
      return this.missing.contains(id);
    }

    public void makeOldRequest() {
      this.processedCount++;
    }

    public int getProcessedCount() {
      return this.processedCount;
    }

    public boolean isNewRequest() {
      return this.processedCount == 0;
    }

    public boolean removeOnRelease() {
      return this.removeOnRelease;
    }

    public ObjectIDSet getLookupIDs() {
      return this.responseContext.getLookupIDs();
    }

    public ObjectIDSet getNewObjectIDs() {
      return this.responseContext.getNewObjectIDs();
    }

    public void setResults(ObjectManagerLookupResults results) {
      this.responseContext.setResults(results);
    }

    public void missingObject(ObjectID oid) {
      this.missing.add(oid);
    }

    public ObjectIDSet getMissingObjectIDs() {
      return this.missing;
    }

    @Override
    public String toString() {
      return "ObjectManagerLookupContext@" + System.identityHashCode(this) + " : [ processed count = "
             + this.processedCount + ", responseContext = " + this.responseContext + ", missing = " + this.missing
             + " ] ";
    }

    public boolean updateStats() {
      // We only want to update the stats the first time we process this request.
      return this.responseContext.updateStats() && isNewRequest();
    }
  }

  private static class WaitForLookupContext implements ObjectManagerResultsContext {

    private final ObjectID    lookupID;
    private final boolean     missingOk;
    private final ObjectIDSet lookupIDs = new ObjectIDSet();
    private boolean           resultSet = false;
    private ManagedObject     result;
    private final boolean     lookupNewObjects;

    public WaitForLookupContext(ObjectID id, boolean missingOk, boolean lookupNewObjects) {
      this.lookupID = id;
      this.missingOk = missingOk;
      this.lookupNewObjects = lookupNewObjects;
      this.lookupIDs.add(id);
    }

    public synchronized ManagedObject getLookedUpObject() {
      while (!this.resultSet) {
        try {
          wait();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
      return this.result;
    }

    public ObjectIDSet getLookupIDs() {
      return this.lookupIDs;
    }

    public ObjectIDSet getNewObjectIDs() {
      if (this.lookupNewObjects) {
        return this.lookupIDs;
      } else {
        return new ObjectIDSet();
      }
    }

    public synchronized void setResults(ObjectManagerLookupResults results) {
      this.resultSet = true;
      assertMissingObjects(results.getMissingObjectIDs());
      Map objects = results.getObjects();
      Assert.assertTrue(objects.size() == 0 || objects.size() == 1);
      if (objects.size() == 1) {
        this.result = (ManagedObject) objects.get(this.lookupID);
        Assert.assertNotNull(this.result);
      }
      notifyAll();
    }

    private void assertMissingObjects(ObjectIDSet missing) {
      if (!this.missingOk && !missing.isEmpty()) { throw new AssertionError("Lookup of non-exisiting objects : "
                                                                            + missing + " " + this); }
    }

    @Override
    public String toString() {
      return "WaitForLookupContext [ " + this.lookupID + ", missingOK = " + this.missingOk + "]";
    }

    public boolean updateStats() {
      return true;
    }

  }

  private static class Pending {
    private final ObjectManagerLookupContext context;
    private final NodeID                     groupingKey;
    private final int                        maxReachableObjects;

    public Pending(NodeID nodeID, ObjectManagerLookupContext context, int maxReachableObjects) {
      this.groupingKey = nodeID;
      this.context = context;
      this.maxReachableObjects = maxReachableObjects;
    }

    @Override
    public String toString() {
      return "ObjectManagerImpl.Pending[groupingKey=" + this.groupingKey + "]";

    }

    public NodeID getNodeID() {
      return this.groupingKey;
    }

    public ObjectManagerLookupContext getRequestContext() {
      return this.context;
    }

    public int getMaxReachableObjects() {
      return this.maxReachableObjects;
    }

  }

  private static class PendingList {
    List<Pending>                pending      = new ArrayList<Pending>();
    Map<ObjectID, List<Pending>> blocked      = new HashMap<ObjectID, List<Pending>>();
    int                          blockedCount = 0;

    public void makeBlocked(ObjectID blockedOid, Pending pd) {
      List<Pending> blockedRequests = this.blocked.get(blockedOid);
      if (blockedRequests == null) {
        blockedRequests = new ArrayList<Pending>(1);
        this.blocked.put(blockedOid, blockedRequests);
      }
      blockedRequests.add(pd);
      this.blockedCount++;
    }

    public boolean isBlocked(ObjectID id) {
      return this.blocked.containsKey(id);
    }

    public void makeUnBlocked(ObjectID id) {
      List<Pending> blockedRequests = this.blocked.remove(id);
      if (blockedRequests != null) {
        this.pending.addAll(blockedRequests);
        this.blockedCount -= blockedRequests.size();
      }
    }

    public List<Pending> getAndResetPendingRequests() {
      List<Pending> rv = this.pending;
      this.pending = new ArrayList<Pending>();
      return rv;
    }

    public void addPending(Pending pd) {
      this.pending.add(pd);
    }

    public int size() {
      return this.pending.size();
    }

    @Override
    public String toString() {
      return "PendingList { pending lookups = " + this.pending.size() + ", blocked count = " + this.blockedCount
             + ", blocked oids = " + this.blocked.keySet() + " } ";
    }
  }

  /*********************************************************************************************************************
   * ManagedObjectChangeListener interface
   */
  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    this.collector.changed(changedObject, oldReference, newReference);
  }

  private static Set<ManagedObjectReference> createNewSet() {
    return new HashSet<ManagedObjectReference>(INITIAL_SET_SIZE, LOAD_FACTOR);
  }

}
