/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.Destroyable;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.NullGarbageCollector;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
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
public class ObjectManagerImpl implements ObjectManager, ManagedObjectChangeListener, PrettyPrintable {

  private static enum LookupState {
    RETRY, NOT_AVAILABLE, AVAILABLE
  }

  private static enum MissingObjects {
    OK, NOT_OK
  }

  private static enum NewObjects {
    LOOKUP, DONT_LOOKUP
  }

  private static enum AccessLevel {
    READ, READ_WRITE
  }

  private static final TCLogger                                 logger          = TCLogging
                                                                                    .getLogger(ObjectManager.class);

  private final PersistentManagedObjectStore                    objectStore;
  private final ConcurrentMap<ObjectID, ManagedObjectReference> references;
  private final AtomicInteger                                   checkedOutCount = new AtomicInteger();
  private final PendingList                                     pending         = new PendingList();
  private final AtomicBoolean                                   inShutdown      = new AtomicBoolean();
  private final ObjectManagerStatsListener                      stats;

  private volatile GarbageCollector                             collector       = new NullGarbageCollector();

  // A Lock that prevents checkouts when some critical operation is going on
  private final ReentrantReadWriteLock                          lock            = new ReentrantReadWriteLock();
  private final Condition                                       signal          = this.lock.writeLock().newCondition();

  private final ClientStateManager                              stateManager;
  private final ObjectManagerConfig                             config;
  private final TransactionProvider                             persistenceTransactionProvider;

  public ObjectManagerImpl(final ObjectManagerConfig config, final ClientStateManager stateManager,
                           final PersistentManagedObjectStore objectStore,
                           final ObjectManagerStatsListener stats, final TransactionProvider persistenceTransactionProvider) {
    this.stats = stats;
    Assert.assertNotNull(objectStore);
    this.config = config;
    this.stateManager = stateManager;
    this.objectStore = objectStore;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.references = new ConcurrentHashMap<ObjectID, ManagedObjectReference>(16384, 0.75f, 256);
  }

  @Override
  public void start() {
    this.collector.start();
  }

  /**
   * Stops the ObjectManager - After this call, none of the objects are allowed to be checked out.
   */
  @Override
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
      final Transaction tx = newTransaction();
      flushAllAndCommit(tx, toFlush);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("collector: ").visit(this.collector).flush();
    out.indent().print("references: ").visit(this.references).flush();
    out.indent().print("checkedOutCount: " + this.checkedOutCount.get()).flush();
    out.indent().print("pending: ").visit(this.pending).flush();
    out.indent().print("objectStore: ").duplicateAndIndent().visit(this.objectStore).flush();
    out.indent().print("stateManager: ").duplicateAndIndent().visit(this.stateManager).flush();

    try {

      final StringBuilder rootBuff = new StringBuilder();
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

  @Override
  public ObjectID lookupRootID(final String name) {
    assertNotInShutdown();
    return this.objectStore.getRootID(name);
  }

  @Override
  public boolean lookupObjectsAndSubObjectsFor(final NodeID nodeID, final ObjectManagerResultsContext responseContext,
                                               final int maxReachableObjects) {
    return basicLookupObjectsFor(nodeID,
                                 new ObjectManagerLookupContext(responseContext, AccessLevel.READ_WRITE),
                                 maxReachableObjects);
  }

  @Override
  public boolean lookupObjectsFor(final NodeID nodeID, final ObjectManagerResultsContext responseContext) {
    return basicLookupObjectsFor(nodeID,
                                 new ObjectManagerLookupContext(responseContext, AccessLevel.READ_WRITE), -1);
  }

  @Override
  public Iterator getRoots() {
    assertNotInShutdown();
    return this.objectStore.getRoots().iterator();
  }

  @Override
  public Iterator getRootNames() {
    assertNotInShutdown();
    return this.objectStore.getRootNames().iterator();
  }

  /**
   * For management use only (see interface documentation)
   */
  @Override
  public ManagedObjectFacade lookupFacade(final ObjectID id, final int limit) throws NoSuchObjectException {
    assertNotInShutdown();

    if (!containsObject(id)) { throw new NoSuchObjectException(id); }

    final ManagedObject object = lookup(id, MissingObjects.OK, NewObjects.DONT_LOOKUP, AccessLevel.READ);
    if (object == null) { throw new NoSuchObjectException(id); }

    try {

      return object.createFacade(limit);
    } finally {
      releaseReadOnly(object);
    }
  }

  private ManagedObject lookup(final ObjectID id, final MissingObjects missingObjects, final NewObjects newObjects,
                               AccessLevel accessLevel) {
    assertNotInShutdown();

    final WaitForLookupContext waitContext = new WaitForLookupContext(id, missingObjects, newObjects);
    final ObjectManagerLookupContext context = new ObjectManagerLookupContext(waitContext, accessLevel);
    basicLookupObjectsFor(ClientID.NULL_ID, context, -1);

    final ManagedObject mo = waitContext.getLookedUpObject();
    if (mo == null) {
      Assert.assertTrue(missingObjects == MissingObjects.OK);
    }
    return mo;
  }

  @Override
  public ManagedObject getObjectByID(final ObjectID id) {
    return lookup(id, MissingObjects.NOT_OK, NewObjects.DONT_LOOKUP, AccessLevel.READ_WRITE);
  }

  @Override
  public ManagedObject getObjectByIDReadOnly(final ObjectID id) {
    return lookup(id, MissingObjects.OK, NewObjects.DONT_LOOKUP, AccessLevel.READ);
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
  private ManagedObjectReference getOrLookupReference(final ObjectID id) {
    ManagedObjectReference rv = getReference(id);

    if (rv == null) {
      ManagedObject mo = objectStore.getObjectByID(id);
      if (mo == null) {
        // Object doesn't exist, bail out early.
        return null;
      } else {
        rv = addNewReference(mo, false);
      }
    }
    return rv;
  }

  private ManagedObjectReference addNewReference(final ManagedObject obj, final boolean isRemoveOnRelease) {
    return addNewReference(obj.getReference(), isRemoveOnRelease);
  }

  private ManagedObjectReference addNewReference(final ManagedObjectReference newReference,
                                                 final boolean removeOnRelease) {
    final ManagedObjectReference ref = this.references.putIfAbsent(newReference.getObjectID(), newReference);
    if (removeOnRelease) {
      newReference.setRemoveOnRelease(removeOnRelease);
    }
    return ref == null ? newReference : ref;
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
      final ManagedObjectReference reference = getOrLookupReference(id);
      if (reference == null) {
        context.missingObject(id);
        continue;
      } else if (!available) {
        continue;
      }

      if (reference.isNew() && !newObjectIDs.contains(id)) {
        available = false;
        blockedObjectID = id;
      } else if (!markReferenced(reference)) {
        available = false;
        blockedObjectID = id;
      } else {
        objects.put(id, reference.getObject());
      }
    }

    if (available) {
      final ObjectIDSet processLater = addReachableObjectsIfNecessary(nodeID, maxReachableObjects, objects);
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
                                                     final Map<ObjectID, ManagedObject> objects) {
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

  @Override
  public void createNewObjects(final Set<ObjectID> newObjectIDs) {
    for (final ObjectID oid : newObjectIDs) {
      final ManagedObject mo = objectStore.createObject(oid);
      createObject(mo);
    }
  }

  @Override
  public void releaseReadOnly(final ManagedObject object) {
    if (this.config.paranoid() && !object.isNew() && object.isDirty()) { throw new AssertionError(
                                                                                                  "Object is dirty after a read-only checkout "
                                                                                                      + object); }
    basicReleaseReadOnly(object);
    postRelease();
  }

  @Override
  public void release(final ManagedObject object) {
    basicRelease(object);
    postRelease();

  }

  @Override
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
  @Override
  public void releaseAll(final Collection<ManagedObject> managedObjects) {
    // flushAllAndCommit(persistenceTransaction, managedObjects);
    for (final ManagedObject managedObject : managedObjects) {
      basicRelease(managedObject);
    }
    postRelease();
  }

  private ObjectIDSet removeAllObjectsByID(final Set<ObjectID> toDelete) {
    ObjectIDSet missingObjects = new ObjectIDSet();
    while(!toDelete.isEmpty()) {
      Iterator<ObjectID> i = toDelete.iterator();
      while (i.hasNext()) {
        ObjectID id = i.next();
        ManagedObjectReference ref = getOrLookupReference(id);
        if (ref == null) {
          missingObjects.add(id);
          i.remove();
          continue;
        }
        if (markReferenced(ref)) {
          if (!ref.isNew()) {
            i.remove();
            objectStore.removeAllObjectsByID(Collections.singleton(id));
            removeReferenceAndDestroyIfNecessary(id);
          }
          unmarkReferenced(ref);
          makeUnBlocked(id);
        }
      }
      processPendingLookups();
    }
    return missingObjects;
  }

  @Override
  public Set<ObjectID> tryDeleteObjects(final Set<ObjectID> objectsToDelete, final Set<ObjectID> checkedOutObjects) {
    Set<ObjectID> retry = new ObjectIDSet();
    for (ObjectID objectID : objectsToDelete) {
      ManagedObjectReference ref = getOrLookupReference(objectID);
      if (checkedOutObjects.contains(objectID)) {
        // If the object is already checked out by this operation, just delete it.
        objectStore.removeAllObjectsByID(Collections.singleton(objectID));
        ref.setRemoveOnRelease(true);
        continue;
      } else if (ref == null || !markReferenced(ref)) {
        // The object either doesn't exist or we failed to mark it, drop it into the retry set.
        retry.add(objectID);
        continue;
      } else if (ref.isNew()) {
        // Don't delete new objects as they haven't been created yet. Fall through since we still need to unmark the object
        retry.add(objectID);
      } else {
        // The object exists and is deletable, delete it.
        objectStore.removeAllObjectsByID(Collections.singleton(objectID));
        removeReferenceAndDestroyIfNecessary(objectID);
      }
      unmarkReferenced(ref);
      makeUnBlocked(objectID);
    }
    processPendingLookups();
    return retry;
  }

  @Override
  public int getCheckedOutCount() {
    return this.checkedOutCount.get();
  }

  @Override
  public Set getRootIDs() {
    return this.objectStore.getRoots();
  }

  @Override
  public Map getRootNamesToIDsMap() {
    return this.objectStore.getRootNamesToIDsMap();
  }

  @Override
  public ObjectIDSet getAllObjectIDs() {
    return this.objectStore.getAllObjectIDs();
  }

  private boolean containsObject(final ObjectID id) {
    return this.objectStore.containsObject(id);
  }

  @Override
  public ObjectIDSet getObjectIDsInCache() {
    final ObjectIDSet ids = new ObjectIDSet();
    ids.addAll(this.references.keySet()); // CDM doesn't throw ConcurrentModificationException
    return ids;
  }

  @Override
  public int getLiveObjectCount() {
    return this.objectStore.getObjectCount();
  }

  @Override
  public Set<ObjectID> getObjectReferencesFrom(final ObjectID id, final boolean cacheOnly) {
    if (this.objectStore.hasNoReferences(id)) {
      return TCCollections.EMPTY_OBJECT_ID_SET;
    }
    final ManagedObjectReference mor = getReference(id);
    if ((mor == null && cacheOnly) || (mor != null && mor.isNew())) {
      // Object either not in cache or is a new object, return emtpy set
      return TCCollections.EMPTY_OBJECT_ID_SET;
    }
    final ManagedObject mo = lookup(id, MissingObjects.OK, NewObjects.LOOKUP, AccessLevel.READ);
    if ( mo == null ) {
      return TCCollections.EMPTY_OBJECT_ID_SET;
    }
    final Set<ObjectID> references2Return = mo.getObjectReferences();
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
    updateNewFlag(object);
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

  private void updateNewFlag(final ManagedObject object) {
    if (object.isNew()) {
      object.setIsNew(false);
    }
  }

  private void removeReferenceIfNecessary(final ManagedObjectReference mor) {
    if (mor.isRemoveOnRelease()) {
      if (mor.getObject().isDirty()) {
        // This could happen if the faulting is initiated by someone with removeOnRelease=true but someone else sneaks
        // in before that guy and gets the object. So adjust the state and proceed.
        logger.info(mor + " is DIRTY but isRemoveOnRelease is true, resetting it");
        mor.setRemoveOnRelease(false);
      } else {
        final ManagedObjectReference removed = this.removeReferenceAndDestroyIfNecessary(mor.getObjectID());

        if (removed == null) { throw new AssertionError("Removed is null : " + mor); }
      }
    }
  }

  private ManagedObjectReference removeReferenceAndDestroyIfNecessary(ObjectID oid) {
    // logger.info("XXX removing reference " + oid);
    final ManagedObjectReference removed = this.references.remove(oid);
    if (removed != null && removed.getObject() != null) {
      ManagedObjectState removedManagedObjectState = removed.getObject().getManagedObjectState();
      if (removedManagedObjectState instanceof Destroyable) {
        ((Destroyable) removedManagedObjectState).destroy();
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

  @Override
  public void waitUntilReadyToGC() {
    this.lock.writeLock().lock();
    try {
      checkAndNotifyGC();
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

  @Override
  public void notifyGCComplete(final DGCResultContext gcResult) {
    this.lock.writeLock().lock();
    try {
      Assert.assertTrue(this.collector.requestGCDeleteStart());
    } finally {
      this.lock.writeLock().unlock();
    }
    Transaction transaction = persistenceTransactionProvider.newTransaction();
    deleteObjects(gcResult.getGarbageIDs());
    transaction.commit();
  }

  @Override
  public Set<ObjectID> deleteObjects(Set<ObjectID> toDelete) {
    return removeAllObjectsByID(new ObjectIDSet(toDelete));
  }
  
  private void flushAllAndCommit(final Transaction persistenceTransaction, final Collection managedObjects) {
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
  }

  public PersistentManagedObjectStore getObjectStore() {
    return this.objectStore;
  }

  public ClientStateManager getClientStateManager() {
    return this.stateManager;
  }

  @Override
  public void createRoot(final String rootName, final ObjectID id) {
    assertNotInShutdown();
    this.objectStore.addNewRoot(null, rootName, id);
    this.stats.newObjectCreated();
    // This change needs to be notified so that new roots are not missed out
    changed(null, null, id);
  }

  private Transaction newTransaction() {
    return this.persistenceTransactionProvider.newTransaction();
  }

  @Override
  public GarbageCollector getGarbageCollector() {
    return this.collector;
  }

  @Override
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

  private static class ObjectManagerLookupContext implements ObjectManagerResultsContext {

    private final ObjectManagerResultsContext responseContext;
    private final ObjectIDSet                 missing        = new ObjectIDSet();
    private final AccessLevel                 accessLevel;
    private int                               processedCount = 0;

    public ObjectManagerLookupContext(final ObjectManagerResultsContext responseContext,
                                      AccessLevel accessLevel) {
      this.responseContext = responseContext;
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

    @Override
    public ObjectIDSet getLookupIDs() {
      return this.responseContext.getLookupIDs();
    }

    @Override
    public ObjectIDSet getNewObjectIDs() {
      return this.responseContext.getNewObjectIDs();
    }

    @Override
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
  }

  private class WaitForLookupContext implements ObjectManagerResultsContext {

    private final ObjectID       lookupID;
    private final ObjectIDSet    lookupIDs = new ObjectIDSet();
    private boolean              resultSet = false;
    private ManagedObject        result;
    private final MissingObjects missingObjects;
    private final NewObjects     newObjects;

    public WaitForLookupContext(final ObjectID id, final MissingObjects missingObjects, final NewObjects newObjects) {
      this.lookupID = id;
      this.missingObjects = missingObjects;
      this.newObjects = newObjects;
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

    @Override
    public ObjectIDSet getLookupIDs() {
      return this.lookupIDs;
    }

    @Override
    public ObjectIDSet getNewObjectIDs() {
      if (this.newObjects == NewObjects.LOOKUP) {
        return this.lookupIDs;
      } else {
        return TCCollections.EMPTY_OBJECT_ID_SET;
      }
    }

    @Override
    public synchronized void setResults(final ObjectManagerLookupResults results) {
      if (this.resultSet) { throw new AssertionError("results already set"); }
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
      return "WaitForLookupContext [ " + this.lookupID + ", " + this.missingObjects + ", " + this.resultSet + "]";
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

    @Override
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
  @Override
  public void changed(final ObjectID changedObject, final ObjectID oldReference, final ObjectID newReference) {
    this.collector.changed(changedObject, oldReference, newReference);
  }
}
