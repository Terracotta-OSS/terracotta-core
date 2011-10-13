/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.l2.handler.DestroyableMapHandler.DestroyableMapContext;
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
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.context.ManagedObjectFlushingContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.PeriodicDGCResultContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.NullGarbageCollector;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.db.TCDestroyable;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.NullTransactionalObjectManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Counter;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages access to all the Managed objects in the system. This class is rewritten to be concurrent.
 */
public class ObjectManagerImpl implements ObjectManager, ManagedObjectChangeListener, Evictable, PrettyPrintable {

  private static enum LookupState {
    RETRY, NOT_AVAILABLE, AVAILABLE
  }

  private static enum MissingObjects {
    OK, NOT_OK
  }

  private static enum UpdateStats {
    UPDATE, DONT_UPDATE
  }

  private static enum NewObjects {
    LOOKUP, DONT_LOOKUP
  }

  private static enum AccessLevel {
    READ, READ_WRITE
  }

  private static final TCLogger                                 logger          = TCLogging
                                                                                    .getLogger(ObjectManager.class);

  private static final int                                      MAX_COMMIT_SIZE = TCPropertiesImpl
                                                                                    .getProperties()
                                                                                    .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_MAXOBJECTS_TO_COMMIT);

  private final ManagedObjectStore                              objectStore;
  private final ConcurrentMap<ObjectID, ManagedObjectReference> references;
  private final EvictionPolicy                                  evictionPolicy;
  private final Counter                                         flushInProgress = new Counter();
  private final AtomicInteger                                   checkedOutCount = new AtomicInteger();
  private final AtomicInteger                                   preFetchedCount = new AtomicInteger();
  private final PendingList                                     pending         = new PendingList();
  private final AtomicBoolean                                   inShutdown      = new AtomicBoolean();

  private GarbageCollector                                      collector       = new NullGarbageCollector();
  private ObjectManagerStatsListener                            stats           = new NullObjectManagerStatsListener();
  private TransactionalObjectManager                            txnObjectMgr    = new NullTransactionalObjectManager();

  // A Lock that prevents checkouts when some critical operation is going on
  private final ReentrantReadWriteLock                          lock            = new ReentrantReadWriteLock();
  private final Condition                                       signal          = this.lock.writeLock().newCondition();

  private final ClientStateManager                              stateManager;
  private final ObjectManagerConfig                             config;
  private final PersistenceTransactionProvider                  persistenceTransactionProvider;
  private final Sink                                            faultSink;
  private final Sink                                            flushSink;

  private final ObjectStatsRecorder                             objectStatsRecorder;
  private final NoReferencesIDStore                             noReferencesIDStore;
  private final Sink                                            destroyableMapSink;

  public ObjectManagerImpl(final ObjectManagerConfig config, final ClientStateManager stateManager,
                           final ManagedObjectStore objectStore, final EvictionPolicy cache,
                           final PersistenceTransactionProvider persistenceTransactionProvider, final Sink faultSink,
                           final Sink flushSink, final ObjectStatsRecorder objectStatsRecorder, Sink destroyableMapSink) {
    this.faultSink = faultSink;
    this.flushSink = flushSink;
    Assert.assertNotNull(objectStore);
    this.config = config;
    this.stateManager = stateManager;
    this.objectStore = objectStore;
    this.evictionPolicy = cache;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.references = new ConcurrentHashMap<ObjectID, ManagedObjectReference>(16384, 0.75f, 256);
    this.objectStatsRecorder = objectStatsRecorder;
    this.noReferencesIDStore = new NoReferencesIDStoreImpl(config.doGC());
    this.destroyableMapSink = destroyableMapSink;
  }

  public void setTransactionalObjectManager(final TransactionalObjectManager txnObjectManager) {
    this.txnObjectMgr = txnObjectManager;
  }

  public void setStatsListener(final ObjectManagerStatsListener statsListener) {
    this.stats = statsListener;
  }

  public void start() {
    this.collector.start();
  }

  /**
   * Stops the ObjectManager - After this call, none of the objects are allowed to be checked out.
   */
  public void stop() {
    if (!this.inShutdown.compareAndSet(false, true)) { return; }

    this.collector.stop();
    if (this.config.paranoid()) { return; }

    this.lock.writeLock().lock(); // Allow no more checkouts
    try {
      // flush the cache to stable persistence.
      final Set<ManagedObject> toFlush = new HashSet<ManagedObject>();
      for (final ManagedObjectReference ref : this.references.values()) {
        final ManagedObject obj = ref.getObject();
        if (!obj.isNew() && !ref.isReferenced() && obj.isDirty()) {
          toFlush.add(obj);
        }
      }
      final PersistenceTransaction tx = newTransaction();
      flushAllAndCommit(tx, toFlush);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("collector: ").visit(this.collector).flush();
    out.indent().print("references: ").visit(this.references).flush();
    out.indent().print("checkedOutCount: " + this.checkedOutCount.get()).flush();
    out.indent().print("prefetched: " + this.preFetchedCount.get()).flush();
    out.indent().print("pending: ").visit(this.pending).flush();
    out.indent().print("objectStore: ").duplicateAndIndent().visit(this.objectStore).flush();
    out.indent().print("stateManager: ").duplicateAndIndent().visit(this.stateManager).flush();

    try {

      final StringBuffer rootBuff = new StringBuffer();
      for (final Iterator rootIter = getRootNames(); rootIter.hasNext();) {
        rootBuff.append(rootIter.next());
        if (rootIter.hasNext()) {
          rootBuff.append(",");
        }
      }
      out.indent().print("roots: " + rootBuff.toString()).println().flush();

    } catch (final Throwable t) {
      logger.error("exception printing roots in ObjectManagerImpl", t);
    }

    return out;
  }

  public ObjectID lookupRootID(final String name) {
    assertNotInShutdown();
    return this.objectStore.getRootID(name);
  }

  public boolean lookupObjectsAndSubObjectsFor(final NodeID nodeID, final ObjectManagerResultsContext responseContext,
                                               final int maxReachableObjects) {
    return basicLookupObjectsFor(nodeID,
                                 new ObjectManagerLookupContext(responseContext, false, AccessLevel.READ_WRITE),
                                 maxReachableObjects);
  }

  public boolean lookupObjectsFor(final NodeID nodeID, final ObjectManagerResultsContext responseContext) {
    return basicLookupObjectsFor(nodeID,
                                 new ObjectManagerLookupContext(responseContext, false, AccessLevel.READ_WRITE), -1);
  }

  public Iterator getRoots() {
    assertNotInShutdown();
    return this.objectStore.getRoots().iterator();
  }

  public Iterator getRootNames() {
    assertNotInShutdown();
    return this.objectStore.getRootNames().iterator();
  }

  /**
   * For management use only (see interface documentation)
   */
  public ManagedObjectFacade lookupFacade(final ObjectID id, final int limit) throws NoSuchObjectException {
    assertNotInShutdown();

    if (!containsObject(id)) { throw new NoSuchObjectException(id); }

    final ManagedObject object = lookup(id, MissingObjects.OK, NewObjects.DONT_LOOKUP, UpdateStats.UPDATE,
                                        AccessLevel.READ);
    if (object == null) { throw new NoSuchObjectException(id); }

    try {

      return object.createFacade(limit);
    } finally {
      releaseReadOnly(object);
    }
  }

  private ManagedObject lookup(final ObjectID id, final MissingObjects missingObjects, final NewObjects newObjects,
                               final UpdateStats updateStats, AccessLevel accessLevel) {
    assertNotInShutdown();

    final WaitForLookupContext waitContext = new WaitForLookupContext(id, missingObjects, newObjects, updateStats);
    final ObjectManagerLookupContext context = new ObjectManagerLookupContext(waitContext, true, accessLevel);
    basicLookupObjectsFor(ClientID.NULL_ID, context, -1);

    final ManagedObject mo = waitContext.getLookedUpObject();
    if (mo == null) {
      Assert.assertTrue(missingObjects == MissingObjects.OK);
    }
    return mo;
  }

  public ManagedObject getObjectByID(final ObjectID id) {
    return lookup(id, MissingObjects.NOT_OK, NewObjects.DONT_LOOKUP, UpdateStats.UPDATE, AccessLevel.READ_WRITE);
  }

  /**
   * This method does not update the cache hit/miss stats. You may want to use this if you have prefetched the objects.
   */
  public ManagedObject getQuietObjectByID(ObjectID id) {
    return lookup(id, MissingObjects.NOT_OK, NewObjects.DONT_LOOKUP, UpdateStats.DONT_UPDATE, AccessLevel.READ_WRITE);
  }

  public ManagedObject getObjectByIDOrNull(final ObjectID id) {
    return lookup(id, MissingObjects.OK, NewObjects.DONT_LOOKUP, UpdateStats.UPDATE, AccessLevel.READ_WRITE);
  }

  private boolean markReferenced(final ManagedObjectReference reference) {
    final boolean marked = reference.markReference();
    if (marked) {
      if (reference != this.references.get(reference.getObjectID())) {
        // This reference was removed by someone else and then unmarked before this thread got a chance to call
        // markReferenced.
        reference.unmarkReference();
        return false;
      }
      this.checkedOutCount.incrementAndGet();
    }
    return marked;
  }

  private void unmarkReferenced(final ManagedObjectReference reference) {
    if (!reference.unmarkReference()) { throw new AssertionError("Attempt to unmark an unreferenced object: "
                                                                 + reference); }
    final int current = this.checkedOutCount.decrementAndGet();
    Assert.assertTrue(current >= 0);
  }

  /**
   * Retrieves materialized references.
   */
  private ManagedObjectReference getReference(final ObjectID id) {
    return this.references.get(id);
  }

  /**
   * Retrieves materialized references -- if not materialized, will initiate a request to materialize them from the
   * object store.
   * 
   * @return null if the object is missing
   */
  private ManagedObjectReference getOrLookupReference(final ObjectManagerLookupContext context, final ObjectID id) {
    ManagedObjectReference rv = getReference(id);

    if (rv == null) {
      rv = initiateFaultingFor(id, context.removeOnRelease());
    } else if (rv instanceof FaultingManagedObjectReference) {
      // Check to see if the retrieve was complete and the Object is missing
      final FaultingManagedObjectReference fmr = (FaultingManagedObjectReference) rv;
      if (!fmr.isFaultingInProgress()) {
        this.references.remove(id, fmr);
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
      if (rv.isCacheManaged()) {
        // TODO:: this is synchronized, see if we can avoid synchronization
        this.evictionPolicy.markReferenced(rv);
      }
    }
    return rv;
  }

  // Request Faulting in a different stage and give back a "Referenced" proxy
  private ManagedObjectReference initiateFaultingFor(final ObjectID id, final boolean removeOnRelease) {

    this.stats.cacheMiss();
    final FaultingManagedObjectReference myRef = new FaultingManagedObjectReference(id);
    final ManagedObjectReference oldRef = addFaultingReference(myRef);
    if (null == oldRef) {
      final ManagedObjectFaultingContext mofc = new ManagedObjectFaultingContext(id, removeOnRelease);
      this.faultSink.add(mofc);
      return myRef;
    }
    return oldRef;
  }

  public void addFaultedObject(final ObjectID oid, final ManagedObject mo, final boolean removeOnRelease) {
    final ManagedObjectReference mor = this.references.get(oid);
    if (mor == null || !(mor instanceof FaultingManagedObjectReference) || !oid.equals(mor.getObjectID())) {
      // Format
      throw new AssertionError("ManagedObjectReference is not what was expected : " + mor + " oid : " + oid
                               + " Faulting in : " + mo);
    }
    final FaultingManagedObjectReference fmor = (FaultingManagedObjectReference) mor;
    if (mo == null) {
      fmor.faultingFailed();
    } else {
      Assert.assertEquals(oid, mo.getID());
      addFaultedReference(mo, removeOnRelease, fmor);
      this.noReferencesIDStore.addToNoReferences(mo);
    }
    makeUnBlocked(oid);
    postRelease();
  }

  public void preFetchObjectsAndCreate(final Set<ObjectID> oids, final Set<ObjectID> newOids) {
    createNewObjects(newOids);
    preFetchObjects(oids);
  }

  private void preFetchObjects(final Set<ObjectID> oids) {
    for (final ObjectID id : oids) {
      final ManagedObjectReference rv = getReference(id);
      if (rv == null) {
        // This object is not in the cache, initiate faulting for the object
        this.preFetchedCount.incrementAndGet();
        initiateFaultingFor(id, false);
      } else {
        this.stats.cacheHit();
      }
    }
  }

  private ManagedObjectReference addNewReference(final ManagedObject obj, final boolean isRemoveOnRelease) {
    return addNewReference(obj.getReference(), isRemoveOnRelease, null);
  }

  private ManagedObjectReference addNewReference(final ManagedObjectReference newReference,
                                                 final boolean removeOnRelease, final ManagedObjectReference expectedOld) {
    final Object oldRef = this.references.put(newReference.getObjectID(), newReference);
    if (oldRef != expectedOld) { throw new AssertionError("Object was not as expected. Reference was not equal to : = "
                                                          + expectedOld + " but was : " + oldRef + " : new = "
                                                          + newReference); }
    if (removeOnRelease) {
      newReference.setRemoveOnRelease(removeOnRelease);
    } else if (newReference.isCacheManaged()) {
      this.evictionPolicy.add(newReference);
    }
    return newReference;
  }

  private ManagedObjectReference addFaultingReference(final FaultingManagedObjectReference reference) {
    return this.references.putIfAbsent(reference.getObjectID(), reference);
  }

  private ManagedObjectReference addFaultedReference(final ManagedObject obj, final boolean isRemoveOnRelease,
                                                     final FaultingManagedObjectReference fr) {
    return addNewReference(obj.getReference(), isRemoveOnRelease, fr);
  }

  private void reapCache(final Collection removalCandidates, final Collection<ManagedObject> toFlush,
                         final Collection<ManagedObjectReference> removedObjects) {
    this.lock.readLock().lock();
    try {
      if (this.collector.isPausingOrPaused()) {
        // TODO::May be nice to just wait till the state changes and proceed
        return;
      }
      for (final Object cand : removalCandidates) {
        final ManagedObjectReference removalCandidate = (ManagedObjectReference) cand;
        if (removalCandidate == null || removalCandidate.isReferenced()) {
          continue;
        }
        if (markReferenced(removalCandidate)) {
          // It is possible that before the cache evictor has a chance to mark the reference, the DGC could come and
          // remove the reference, hence we check in references map again - this order of checking is important
          final ObjectID id = removalCandidate.getObjectID();
          if (this.references.containsKey(id)) {
            this.evictionPolicy.remove(removalCandidate);
            if (removalCandidate.getObject().isDirty()) {
              Assert.assertFalse(this.config.paranoid());
              toFlush.add(removalCandidate.getObject());
            } else {
              // paranoid mode or the object is not dirty - just remove from reference
              final ManagedObjectReference inMap = this.removeReferenceAndDestroyIfNecessary(id);
              if (inMap != removalCandidate) { throw new AssertionError("Not the same element : removalCandidate : "
                                                                        + removalCandidate + " inMap : " + inMap); }
              removedObjects.add(removalCandidate);
              unmarkReferenced(removalCandidate);
              makeUnBlocked(id);
            }
          } else {
            unmarkReferenced(removalCandidate);
          }
        }
      }
      notifyCollectorEvictedObjects(toFlush);
      notifyCollectorEvictedObjects(removedObjects);
    } finally {
      this.lock.readLock().unlock();
    }
  }

  private void notifyCollectorEvictedObjects(final Collection evicted) {
    this.collector.notifyObjectsEvicted(evicted);
  }

  private void evicted(final Collection managedObjects) {
    for (final Iterator i = managedObjects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      final ObjectID oid = mo.getID();
      final Object o = this.removeReferenceAndDestroyIfNecessary(oid);
      if (o == null) {
        logger.warn("Object ID : " + mo.getID() + " was mapped to null but should have been mapped to a reference of  "
                    + mo);
      }
      unmarkReferenced(mo.getReference());
      // Remove first and then unblock to make sure we don't miss any request
      makeUnBlocked(oid);
    }
    postRelease();
  }

  private boolean basicLookupObjectsFor(final NodeID nodeID, final ObjectManagerLookupContext context,
                                        final int maxReachableObjects) {
    assertNotInShutdown();

    this.lock.readLock().lock();
    try {
      // DEV-5889 : Allowing READ requests like DGC and lookupFacade to go thru as blocking those might result in a
      // deadlock.
      if (context.getAccessLevel() == AccessLevel.READ_WRITE && this.collector.isPausingOrPaused()) {
        makePending(nodeID, context, maxReachableObjects);
        return false;
      }

      LookupState result;
      int retrys = 0;
      do {
        result = basicInternalLookupObjectsFor(nodeID, context, maxReachableObjects);
        if (result == LookupState.RETRY && retrys++ % 10 == 9) {
          logger.warn("Very high contention : retried lookup for " + nodeID + "," + context + " for " + retrys
                      + "times");
        }
      } while (result == LookupState.RETRY);
      return (result == LookupState.AVAILABLE);
    } finally {
      this.lock.readLock().unlock();
    }
  }

  private LookupState basicInternalLookupObjectsFor(final NodeID nodeID, final ObjectManagerLookupContext context,
                                                    final int maxReachableObjects) {
    final Map<ObjectID, ManagedObject> objects = new HashMap<ObjectID, ManagedObject>();
    ObjectID blockedObjectID = ObjectID.NULL_ID;
    boolean available = true;

    final Set<ObjectID> newObjectIDs = context.getNewObjectIDs();
    final SortedSet<ObjectID> ids = context.getLookupIDs(); // A sorted set to maintain lock order
    for (final ObjectID id : ids) {
      if (context.isMissingObject(id)) {
        continue;
      }

      // We don't check available flag before doing calling getOrLookupReference() for two reasons.
      // 1) To get the right hit/miss count and
      // 2) to Fault objects that are not available
      final ManagedObjectReference reference = getOrLookupReference(context, id);
      if (reference == null || !available) {
        continue;
      }

      if (isANewObjectIn(reference, newObjectIDs) && markReferenced(reference)) {
        objects.put(id, reference.getObject());
      } else {
        available = false;
        // Setting only the first referenced object to process Pending. If objects are being faulted in, then this
        // will ensure that we don't run processPending multiple times unnecessarily.
        blockedObjectID = id;
      }
    }

    if (available) {
      final ObjectIDSet processLater = addReachableObjectsIfNecessary(nodeID, maxReachableObjects, objects,
                                                                      newObjectIDs);
      final ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(objects, processLater,
                                                                                    context.getMissingObjectIDs());
      context.setResults(results);
      return LookupState.AVAILABLE;
    } else {
      context.makeOldRequest();
      unmarkReferenced(objects.values());
      // It is OK to not unblock these unmarked references as any request that is blocked by these objects will be
      // processed after this request is (unblocked) and processed
      return addBlocked(nodeID, context, maxReachableObjects, blockedObjectID);
    }
  }

  private ObjectIDSet addReachableObjectsIfNecessary(final NodeID nodeID, final int maxReachableObjects,
                                                     final Map<ObjectID, ManagedObject> objects,
                                                     final Set<ObjectID> newObjectIDs) {
    if (maxReachableObjects <= 0) { return TCCollections.EMPTY_OBJECT_ID_SET; }
    final ManagedObjectTraverser traverser = new ManagedObjectTraverser(maxReachableObjects);
    Collection<ManagedObject> lookedUpObjects = objects.values();
    do {
      traverser.traverse(lookedUpObjects);
      lookedUpObjects = new ArrayList<ManagedObject>();
      final Set<ObjectID> lookupObjectIDs = traverser.getObjectsToLookup();
      if (lookupObjectIDs.isEmpty()) {
        break;
      }
      this.stateManager.removeReferencedFrom(nodeID, lookupObjectIDs);
      for (final ObjectID id : lookupObjectIDs) {
        if (objects.size() >= maxReachableObjects) {
          break;
        }
        final ManagedObjectReference newRef = getReference(id);
        // Note : Objects are looked up only if it is in the memory and not referenced and is not New
        if (newRef != null && !newRef.isNew() && markReferenced(newRef)) {
          if (objects.put(id, newRef.getObject()) == null) {
            lookedUpObjects.add(newRef.getObject());
          }
        }
      }
    } while (objects.size() < maxReachableObjects);
    return traverser.getPendingObjectsToLookup(lookedUpObjects);
  }

  private boolean isANewObjectIn(final ManagedObjectReference reference, final Set<ObjectID> newObjectIDs) {
    // If reference isNew() and not in newObjects, someone (L1) is trying to do a lookup before the object is fully
    // created, make it pending.
    if (reference.isNew() && !newObjectIDs.contains(reference.getObjectID())) { return false; }
    return true;
  }

  private void unmarkReferenced(final Collection<ManagedObject> mos) {
    for (final ManagedObject managedObject : mos) {
      unmarkReferenced(managedObject.getReference());
    }
  }

  private LookupState addBlocked(final NodeID nodeID, final ObjectManagerLookupContext context,
                                 final int maxReachableObjects, final ObjectID blockedOid) {
    final Pending p = new Pending(nodeID, context, maxReachableObjects);
    this.pending.makeBlocked(blockedOid, p);
    if (context.getProcessedCount() % 500 == 499) {
      logger.warn("Reached " + context.getProcessedCount() + " Pending size : " + this.pending.size()
                  + " : basic look up for : " + context + " maxReachable depth : " + maxReachableObjects);
    }
    // Reverify to make sure that the state hasn't changed while we are adding to pending
    final ManagedObjectReference ref = getReference(blockedOid);
    if (ref == null || (!ref.isNew() && !ref.isReferenced())) {
      // Things changed - retry (we check for null, as it could be flushed to disk or DGCed
      if (this.pending.removeBlocked(blockedOid, p)) {
        // Nobody picked this task up in the meantime, so retry
        return LookupState.RETRY;
      }
    }
    return LookupState.NOT_AVAILABLE;
  }

  public void createNewObjects(final Set<ObjectID> newObjectIDs) {
    for (final ObjectID oid : newObjectIDs) {
      final ManagedObject mo = new ManagedObjectImpl(oid);
      createObject(mo);
    }
  }

  public void releaseReadOnly(final ManagedObject object) {
    if (this.config.paranoid() && !object.isNew() && object.isDirty()) { throw new AssertionError(
                                                                                                  "Object is dirty after a read-only checkout "
                                                                                                      + object); }
    basicReleaseReadOnly(object);
    postRelease();
  }

  public void releaseAndCommit(final PersistenceTransaction persistenceTransaction, final ManagedObject object) {
    if (this.config.paranoid()) {
      flushAndCommit(persistenceTransaction, object);
    }
    basicRelease(object);
    postRelease();

  }

  public void releaseAllReadOnly(final Collection<ManagedObject> objects) {
    for (final ManagedObject mo : objects) {
      if (this.config.paranoid() && !mo.isNew() && mo.isDirty()) {
        // It is possible to release new just created objects before it has a chance to get applied because of a recall
        // due to a DGC. Check out ObjectManagerTest.testRecallNewObjects()
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
   * releasing the objects.
   */
  public void releaseAllAndCommit(final PersistenceTransaction persistenceTransaction,
                                  final Collection<ManagedObject> managedObjects) {
    if (this.config.paranoid()) {
      flushAllAndCommit(persistenceTransaction, managedObjects);
    }
    for (final ManagedObject managedObject : managedObjects) {
      basicRelease(managedObject);
    }
    postRelease();
  }

  private void removeAllObjectsByID(final Set<ObjectID> toDelete) {
    Set<ObjectID> referenced = null;
    this.lock.readLock().lock();
    try {
      for (final ObjectID id : toDelete) {
        ManagedObjectReference ref = this.references.get(id);
        if (ref != null) {
          if (markReferenced(ref)) {
            removeReferenceAndDestroyIfNecessary(id);
            unmarkReferenced(ref);
            makeUnBlocked(id);

          } else {
            // This is possible if the cache manager is evicting this *unreachable* object or the admin console is
            // looking up this object or with eventual consistency another node is looking up/faulting this object.
            logger.warn("Reference : " + ref + " is referenced by someone. So waiting to remove !");
            if (referenced == null) {
              referenced = new ObjectIDSet();
            }
            referenced.add(id);
            continue;
          }
        }
        this.noReferencesIDStore.clearFromNoReferencesStore(id);
        if (ref != null) {
          if (ref.isCacheManaged()) {
            this.evictionPolicy.remove(ref);
          } else if (ref.isNew()) { throw new AssertionError("DGCed Reference is still new : " + ref); }
        }
      }
    } finally {
      this.lock.readLock().unlock();
    }
    if (referenced != null) {
      logger.warn("References : " + referenced + " are referenced by someone. So waiting to remove !");
      lockAndwait(1000, TimeUnit.MILLISECONDS);
      removeAllObjectsByID(referenced);
    }
  }

  private void lockAndwait(int time, TimeUnit unit) {
    this.lock.writeLock().lock();
    try {
      wait(time, unit);
    } finally {
      this.lock.writeLock().unlock();
    }

  }

  public int getCheckedOutCount() {
    return this.checkedOutCount.get();
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

  private boolean containsObject(final ObjectID id) {
    return this.objectStore.containsObject(id);
  }

  public ObjectIDSet getObjectIDsInCache() {
    final ObjectIDSet ids = new ObjectIDSet();
    ids.addAll(this.references.keySet()); // CDM doesn't throw ConcurrentModificationException
    return ids;
  }

  public int getLiveObjectCount() {
    return this.objectStore.getObjectCount();
  }

  public int getCachedObjectCount() {
    return references_size();
  }

  public Set<ObjectID> getObjectReferencesFrom(final ObjectID id, final boolean cacheOnly) {
    if (this.noReferencesIDStore.hasNoReferences(id)) { return TCCollections.EMPTY_OBJECT_ID_SET; }
    final ManagedObjectReference mor = getReference(id);
    if ((mor == null && cacheOnly) || (mor != null && mor.isNew())) {
      // Object either not in cache or is a new object, return emtpy set
      return TCCollections.EMPTY_OBJECT_ID_SET;
    }
    final ManagedObject mo = lookup(id, MissingObjects.NOT_OK, NewObjects.LOOKUP, UpdateStats.UPDATE, AccessLevel.READ);
    final Set references2Return = mo.getObjectReferences();
    releaseReadOnly(mo);
    return references2Return;
  }

  private void postRelease() {
    if (this.collector.isPausingOrPaused()) {
      checkAndNotifyGC();
    } else {
      processPendingLookups();
    }
  }

  private void basicRelease(final ManagedObject object) {
    final ManagedObjectReference mor = object.getReference();
    updateNewFlagAndCreateIfNecessary(object);
    removeReferenceIfNecessary(mor);
    unmarkReferenced(mor);
    makeUnBlocked(object.getID());
  }

  private void basicReleaseReadOnly(final ManagedObject object) {
    final ManagedObjectReference mor = object.getReference();
    removeReferenceIfNecessary(mor);
    unmarkReferenced(mor);
    makeUnBlocked(object.getID());
  }

  private void updateNewFlagAndCreateIfNecessary(final ManagedObject object) {
    if (object.isNew()) {
      this.objectStore.addNewObject(object);
      this.noReferencesIDStore.addToNoReferences(object);
      object.setIsNew(false);
      addToCacheIfNecessary(object.getReference());
      fireNewObjectinitialized(object.getID());
    }
  }

  private void addToCacheIfNecessary(final ManagedObjectReference mor) {
    if (mor.isCacheManaged()) {
      this.evictionPolicy.add(mor);
    }
  }

  private void fireNewObjectinitialized(final ObjectID id) {
    this.collector.notifyNewObjectInitalized(id);
  }

  private void removeReferenceIfNecessary(final ManagedObjectReference mor) {
    if (mor.isRemoveOnRelease()) {
      if (mor.getObject().isDirty()) {
        // This could happen if the faulting is initiated by someone with removeOnRelease=true but someone else sneaks
        // in before that guy and gets the object. So adjust the state and proceed.
        logger.info(mor + " is DIRTY but isRemoveOnRelease is true, resetting it");
        mor.setRemoveOnRelease(false);
      } else {
        final Object removed = this.removeReferenceAndDestroyIfNecessary(mor.getObjectID());
        if (removed == null) { throw new AssertionError("Removed is null : " + mor); }
      }
    }
  }

  private ManagedObjectReference removeReferenceAndDestroyIfNecessary(ObjectID oid) {
    final ManagedObjectReference removed = this.references.remove(oid);
    if (removed != null && removed.getObject() != null) {
      ManagedObjectState removedManagedObjectState = removed.getObject().getManagedObjectState();
      if (removedManagedObjectState instanceof TCDestroyable) {
        destroyableMapSink.add(new DestroyableMapContext((TCDestroyable) removedManagedObjectState));
      }
    }
    return removed;
  }

  private void checkAndNotifyGC() {
    if (this.checkedOutCount.get() == 0) {
      this.collector.notifyReadyToGC();
      signal();
    }
  }

  private void signal() {
    this.lock.writeLock().lock();
    try {
      this.signal.signalAll();
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  public void waitUntilReadyToGC() {
    this.lock.writeLock().lock();
    try {
      checkAndNotifyGC();
      this.txnObjectMgr.recallAllCheckedoutObject();
      int count = 0;
      while (!this.collector.isPaused()) {
        if (count++ % 4 == 3) {
          logger
              .warn("Still waiting for object to be checked back in. collector state is not paused. checkout count = "
                    + this.checkedOutCount.get() + " count  = " + count);
        }
        wait(10000, TimeUnit.MILLISECONDS);
      }
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  private void wait(final int time, final TimeUnit unit) {
    try {
      this.signal.await(time, unit);
    } catch (final InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public void notifyGCComplete(final PeriodicDGCResultContext gcResult) {
    Assert.assertFalse(this.collector.isPausingOrPaused());
    deleteObjects(gcResult);
  }

  public void deleteObjects(final DGCResultContext dgcResultContext) {
    final Set<ObjectID> toDelete = dgcResultContext.getGarbageIDs();
    if (!toDelete.isEmpty()) {
      removeAllObjectsByID(toDelete);
      this.objectStore.removeAllObjectsByID(dgcResultContext);
      // Process pending, since we disabled process pending while GC pause was initiate.
      processPendingLookups();
    }
  }

  private void flushAndCommit(final PersistenceTransaction persistenceTransaction, final ManagedObject managedObject) {
    this.objectStore.commitObject(persistenceTransaction, managedObject);
    persistenceTransaction.commit();
  }

  private void flushAllAndCommit(final PersistenceTransaction persistenceTransaction, final Collection managedObjects) {
    this.objectStore.commitAllObjects(persistenceTransaction, managedObjects);
    persistenceTransaction.commit();
  }

  // This method is for tests only
  public boolean isReferenced(final ObjectID id) {
    final ManagedObjectReference reference = getReference(id);
    return reference != null && reference.isReferenced();
  }

  // This method is public for testing purpose
  public void createObject(final ManagedObject object) {
    assertNotInShutdown();
    final ObjectID oid = object.getID();
    Assert.eval(oid.toLong() != -1);
    addNewReference(object, false);

    this.stats.newObjectCreated();
    fireObjectCreated(oid);
  }

  private void fireObjectCreated(final ObjectID id) {
    this.collector.notifyObjectCreated(id);
  }

  public ManagedObjectStore getObjectStore() {
    return this.objectStore;
  }

  public ClientStateManager getClientStateManager() {
    return this.stateManager;
  }

  public void createRoot(final String rootName, final ObjectID id) {
    assertNotInShutdown();
    final PersistenceTransaction tx = newTransaction();
    this.objectStore.addNewRoot(tx, rootName, id);
    tx.commit();
    this.stats.newObjectCreated();
    // This change needs to be notified so that new roots are not missed out
    changed(null, null, id);
  }

  private PersistenceTransaction newTransaction() {
    return this.persistenceTransactionProvider.newTransaction();
  }

  public GarbageCollector getGarbageCollector() {
    return this.collector;
  }

  public void setGarbageCollector(final GarbageCollector newCollector) {
    assertNotInShutdown();
    if (this.collector != null) {
      this.collector.stop();
    }
    this.collector = newCollector;
  }

  private void processPendingLookups() {
    if (this.pending.size() == 0) { return; }
    final List<Pending> pendingLookups = this.pending.drain();
    for (final Pending p : pendingLookups) {
      basicLookupObjectsFor(p.getNodeID(), p.getRequestContext(), p.getMaxReachableObjects());
    }
  }

  private void makeUnBlocked(final ObjectID id) {
    this.pending.makeUnBlocked(id);
  }

  private void makePending(final NodeID nodeID, final ObjectManagerLookupContext context, final int maxReachableObjects) {
    this.pending.addPending(new Pending(nodeID, context, maxReachableObjects));
  }

  private void assertNotInShutdown() {
    if (this.inShutdown.get()) { throw new ShutdownError(); }
  }

  public void evictCache(final CacheStats stat) {
    final int size = references_size();
    final int toEvict = stat.getObjectCountToEvict(size);
    if (toEvict <= 0) { return; }

    // This could be a costly call, so call just once
    Collection removalCandidates = this.evictionPolicy.getRemovalCandidates(toEvict);

    HashSet<ManagedObject> toFlush = new HashSet<ManagedObject>();
    ArrayList<ManagedObjectReference> removed = new ArrayList<ManagedObjectReference>();
    reapCache(removalCandidates, toFlush, removed);

    if (this.objectStatsRecorder.getFlushDebug()) {
      updateFlushStats(toFlush, removed);
    }

    final int evicted = (toFlush.size() + removed.size());
    final boolean doPostRelease = !removed.isEmpty();

    this.stats.flushed(evicted);

    // Let GC work for us
    removed = null;
    removalCandidates = null;

    if (!toFlush.isEmpty()) {
      initateFlushRequest(toFlush);
      toFlush = null; // make GC work
      waitUntilFlushComplete();
    }

    // TODO:: Send the right objects to the cache manager
    stat.objectEvicted(evicted, references_size(), Collections.EMPTY_LIST, true);
    if (doPostRelease) {
      // Only running postRelease when necessary to avoid hogging memory manager thread here.
      postRelease();
    }
  }

  private void updateFlushStats(final Collection<ManagedObject> toFlush,
                                final Collection<ManagedObjectReference> removedObjects) {
    final Iterator<ManagedObject> flushIter = toFlush.iterator();
    while (flushIter.hasNext()) {
      String className = flushIter.next().getManagedObjectState().getClassName();
      if (className == null) {
        className = "UNKNOWN";
      }
      this.objectStatsRecorder.updateFlushStats(className);
    }
    final Iterator<ManagedObjectReference> removedIter = removedObjects.iterator();
    while (removedIter.hasNext()) {
      String className = removedIter.next().getObject().getManagedObjectState().getClassName();
      if (className == null) {
        className = "UNKNOWN";
      }
      this.objectStatsRecorder.updateFlushStats(className);
    }
  }

  private void waitUntilFlushComplete() {
    this.flushInProgress.waitUntil(0);
  }

  private void initateFlushRequest(final Collection toFlush) {
    this.flushInProgress.increment(toFlush.size());
    for (final Iterator i = toFlush.iterator(); i.hasNext();) {
      int count = 0;
      final ManagedObjectFlushingContext mofc = new ManagedObjectFlushingContext();
      while (count < MAX_COMMIT_SIZE && i.hasNext()) {
        mofc.addObjectToFlush(i.next());
        count++;
        // i.remove();
      }
      this.flushSink.add(mofc);
    }
  }

  public void flushAndEvict(final List objects2Flush) {
    final PersistenceTransaction tx = newTransaction();
    final int size = objects2Flush.size();
    flushAllAndCommit(tx, objects2Flush);
    evicted(objects2Flush);
    this.flushInProgress.decrement(size);
  }

  // TODO:: May have to Optimize it with an atomic integer since size on CHM with 256 segments is costly
  private int references_size() {
    return this.references.size();
  }

  private static class ObjectManagerLookupContext implements ObjectManagerResultsContext {

    private final ObjectManagerResultsContext responseContext;
    private final boolean                     removeOnRelease;
    private final ObjectIDSet                 missing        = new ObjectIDSet();
    private final AccessLevel                 accessLevel;
    private int                               processedCount = 0;

    public ObjectManagerLookupContext(final ObjectManagerResultsContext responseContext, final boolean removeOnRelease,
                                      AccessLevel accessLevel) {
      this.responseContext = responseContext;
      this.removeOnRelease = removeOnRelease;
      this.accessLevel = accessLevel;
    }

    public boolean isMissingObject(final ObjectID id) {
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

    public void setResults(final ObjectManagerLookupResults results) {
      this.responseContext.setResults(results);
    }

    public void missingObject(final ObjectID oid) {
      this.missing.add(oid);
    }

    public ObjectIDSet getMissingObjectIDs() {
      return this.missing;
    }

    public AccessLevel getAccessLevel() {
      return this.accessLevel;
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

    private final ObjectID       lookupID;
    private final ObjectIDSet    lookupIDs = new ObjectIDSet();
    private boolean              resultSet = false;
    private ManagedObject        result;
    private final MissingObjects missingObjects;
    private final NewObjects     newObjects;
    private final UpdateStats    updateStats;

    public WaitForLookupContext(final ObjectID id, final MissingObjects missingObjects, final NewObjects newObjects,
                                UpdateStats updateStats) {
      this.lookupID = id;
      this.missingObjects = missingObjects;
      this.newObjects = newObjects;
      this.updateStats = updateStats;
      this.lookupIDs.add(id);
    }

    public synchronized ManagedObject getLookedUpObject() {
      while (!this.resultSet) {
        try {
          wait();
        } catch (final InterruptedException e) {
          throw new AssertionError(e);
        }
      }
      return this.result;
    }

    public ObjectIDSet getLookupIDs() {
      return this.lookupIDs;
    }

    public ObjectIDSet getNewObjectIDs() {
      if (this.newObjects == NewObjects.LOOKUP) {
        return this.lookupIDs;
      } else {
        return TCCollections.EMPTY_OBJECT_ID_SET;
      }
    }

    public synchronized void setResults(final ObjectManagerLookupResults results) {
      this.resultSet = true;
      assertMissingObjects(results.getMissingObjectIDs());
      final Map objects = results.getObjects();
      Assert.assertTrue(objects.size() == 0 || objects.size() == 1);
      if (objects.size() == 1) {
        this.result = (ManagedObject) objects.get(this.lookupID);
        Assert.assertNotNull(this.result);
      }
      notifyAll();
    }

    private void assertMissingObjects(final ObjectIDSet missing) {
      if (this.missingObjects == MissingObjects.NOT_OK && !missing.isEmpty()) { throw new AssertionError(
                                                                                                         "Lookup of non-existing objects : "
                                                                                                             + missing
                                                                                                             + " "
                                                                                                             + this); }
    }

    @Override
    public String toString() {
      return "WaitForLookupContext [ " + this.lookupID + ", " + this.missingObjects + "]";
    }

    public boolean updateStats() {
      return updateStats == UpdateStats.UPDATE;
    }

  }

  private static class Pending {
    private final ObjectManagerLookupContext context;
    private final NodeID                     groupingKey;
    private final int                        maxReachableObjects;

    public Pending(final NodeID nodeID, final ObjectManagerLookupContext context, final int maxReachableObjects) {
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

  private static class PendingList implements PrettyPrintable {
    private final Queue<Pending>                          pending      = new LinkedBlockingQueue<Pending>();
    private final TCConcurrentMultiMap<ObjectID, Pending> blocked      = new TCConcurrentMultiMap<ObjectID, Pending>(
                                                                                                                     256,
                                                                                                                     0.75f,
                                                                                                                     32);
    private final AtomicInteger                           blockedCount = new AtomicInteger();

    public void makeBlocked(final ObjectID blockedOid, final Pending pd) {
      this.blocked.add(blockedOid, pd);
      this.blockedCount.incrementAndGet();
    }

    public List<Pending> drain() {
      final ArrayList<Pending> drained = new ArrayList<Pending>();
      Pending p;
      while ((p = this.pending.poll()) != null) {
        drained.add(p);
      }
      return drained;
    }

    public boolean removeBlocked(final ObjectID blockedOid, final Pending pd) {
      final boolean success = this.blocked.remove(blockedOid, pd);
      if (success) {
        this.blockedCount.decrementAndGet();
      }
      return success;
    }

    public void makeUnBlocked(final ObjectID id) {
      final Set<Pending> blockedRequests = this.blocked.removeAll(id);
      if (blockedRequests.isEmpty()) { return; }

      for (final Pending pendingRequests : blockedRequests) {
        this.pending.add(pendingRequests);
      }
      this.blockedCount.addAndGet(-blockedRequests.size());
    }

    public void addPending(final Pending pd) {
      this.pending.add(pd);
    }

    public int size() {
      return this.pending.size();
    }

    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      out.print(this.getClass().getName()).flush();
      out.indent().print("pending lookups : ").visit(this.pending.size()).flush();
      out.indent().print("blocked count   : ").visit(this.blockedCount.get()).flush();
      out.indent().print("blocked         : ").visit(this.blocked).flush();
      out.indent().print("pending         : ").visit(this.pending).flush();
      return out;
    }

  }

  /*********************************************************************************************************************
   * ManagedObjectChangeListener interface
   */
  public void changed(final ObjectID changedObject, final ObjectID oldReference, final ObjectID newReference) {
    this.collector.changed(changedObject, oldReference, newReference);
  }
}
