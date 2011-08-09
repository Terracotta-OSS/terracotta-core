/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfStoreValue;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class L1ServerMapLocalCacheManagerImpl implements L1ServerMapLocalCacheManager {

  private static final boolean                                   PINNING_ENABLED         = TCPropertiesImpl
                                                                                             .getProperties()
                                                                                             .getBoolean(
                                                                                                         TCPropertiesConsts.L1_LOCKMANAGER_PINNING_ENABLED);

  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache> localCaches             = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  private final TCConcurrentMultiMap<LockID, ObjectID>           lockIdsToCdsmIds        = new TCConcurrentMultiMap<LockID, ObjectID>();
  private final Map<L1ServerMapLocalCacheStore, Object>          stores                  = new IdentityHashMap<L1ServerMapLocalCacheStore, Object>();
  private final GlobalL1ServerMapLocalCacheStoreListener         localCacheStoreListener = new GlobalL1ServerMapLocalCacheStoreListener();
  private final AtomicBoolean                                    shutdown                = new AtomicBoolean();
  private final LocksRecallService                               locksRecallHelper;
  private final Sink                                             capacityEvictionSink;
  private final RemoveCallback                                   removeCallback;

  private final ObjectIDSet                                      tcObjectSelfStoreOids   = new ObjectIDSet();
  private final ReentrantReadWriteLock                           tcObjectStoreLock       = new ReentrantReadWriteLock();
  private final AtomicInteger                                    tcObjectSelfStoreSize   = new AtomicInteger();
  private volatile TCObjectSelfCallback                          tcObjectSelfRemovedFromStoreCallback;
  private final Map<ObjectID, TCObjectSelf>                      tcObjectSelfTempCache   = new HashMap<ObjectID, TCObjectSelf>();
  private volatile ClientLockManager                             lockManager;

  private static final TCLogger                                  logger                  = TCLogging
                                                                                             .getLogger(L1ServerMapLocalCacheManagerImpl.class);

  public L1ServerMapLocalCacheManagerImpl(LocksRecallService locksRecallHelper, Sink capacityEvictionSink) {
    this.locksRecallHelper = locksRecallHelper;
    this.capacityEvictionSink = capacityEvictionSink;
    removeCallback = new RemoveCallback();
  }

  public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
    this.tcObjectSelfRemovedFromStoreCallback = callback;
  }

  public void setLockManager(ClientLockManager lockManager) {
    this.lockManager = lockManager;
  }

  public ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager, Manager manager,
                                                   boolean localCacheEnabled) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }
    ServerMapLocalCache serverMapLocalCache = localCaches.get(mapId);
    if (serverMapLocalCache == null) {
      serverMapLocalCache = new ServerMapLocalCacheImpl(mapId, objectManager, manager, this, localCacheEnabled,
                                                        removeCallback);
      ServerMapLocalCache old = localCaches.putIfAbsent(mapId, serverMapLocalCache);
      if (old != null) {
        serverMapLocalCache = old;
      }
    }
    return serverMapLocalCache;
  }

  public void addStoreListener(L1ServerMapLocalCacheStore store) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }

    tcObjectStoreLock.writeLock().lock();
    try {
      if (!stores.containsKey(store)) {
        store.addListener(localCacheStoreListener);
        stores.put(store, null);
      }
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  private void throwAlreadyShutdownException() {
    throw new TCRuntimeException("GlobalCacheManager is already shut down.");
  }

  // TODO: is this method needed?
  public void removeLocalCache(ObjectID mapID) {
    localCaches.remove(mapID);
  }

  public void recallLocks(Set<LockID> lockIds) {
    if (PINNING_ENABLED) {
      for (LockID lockId : lockIds) {
        this.lockManager.unpinLock(lockId);
      }
    }

    locksRecallHelper.recallLocks(lockIds);
  }

  public void recallLocksInline(Set<LockID> lockIds) {
    if (PINNING_ENABLED) {
      for (LockID lockId : lockIds) {
        this.lockManager.unpinLock(lockId);
      }
    }

    locksRecallHelper.recallLocksInline(lockIds);
  }

  public void addAllObjectIDsToValidate(Invalidations invalidations) {
    for (ServerMapLocalCache localCache : localCaches.values()) {
      localCache.addAllObjectIDsToValidate(invalidations);
    }
  }

  /**
   * This method is called only for invalidations
   */
  public void removeEntriesForObjectId(ObjectID mapID, Set<ObjectID> set) {
    ServerMapLocalCache cache = localCaches.get(mapID);
    if (cache != null) {
      for (ObjectID id : set) {
        cache.removeEntriesForObjectId(id);
      }
    }
  }

  /**
   * This method is called only when recall happens
   */
  public void removeEntriesForLockId(LockID lockID) {
    if (PINNING_ENABLED) {
      this.lockManager.unpinLock(lockID);
    }

    final Set<ObjectID> cdsmIds = lockIdsToCdsmIds.removeAll(lockID);

    for (ObjectID mapID : cdsmIds) {
      ServerMapLocalCache localCache = localCaches.get(mapID);
      localCache.removeEntriesForLockId(lockID);
    }
  }

  public void rememberMapIdForValueLockId(LockID valueLockId, ObjectID mapID) {
    if (PINNING_ENABLED) {
      this.lockManager.pinLock(valueLockId);
    }

    lockIdsToCdsmIds.add(valueLockId, mapID);
  }

  public void evictElements(Map evictedElements) {
    Set<Map.Entry> entries = evictedElements.entrySet();

    for (Entry entry : entries) {
      if (!(entry.getValue() instanceof AbstractLocalCacheStoreValue)) {
        continue;
      }

      AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) entry.getValue();
      ObjectID mapID = value.getMapID();
      ServerMapLocalCache localCache = localCaches.get(mapID);
      if (localCache != null) {
        // the entry has been already removed from the local store, this will remove the id->key mapping if it exists
        localCache.evictedFromStore(value.getId(), entry.getKey(), value);
      } else {
        throwAssert("LocalCache not mapped for mapId: " + mapID);
      }
    }
  }

  public void shutdown() {
    shutdown.set(true);
    for (L1ServerMapLocalCacheStore store : stores.keySet()) {
      store.clear();
    }
  }

  private class GlobalL1ServerMapLocalCacheStoreListener<K, V> implements L1ServerMapLocalCacheStoreListener<K, V> {

    public void notifyElementEvicted(K key, V value) {
      notifyElementsEvicted(Collections.singletonMap(key, value));
    }

    // TODO: does this need to be present in the interface? not called from outside
    public void notifyElementsEvicted(Map<K, V> evictedElements) {
      // This should be inside another thread, if not it will cause a deadlock
      L1ServerMapEvictedElementsContext context = new L1ServerMapEvictedElementsContext(
                                                                                        evictedElements,
                                                                                        L1ServerMapLocalCacheManagerImpl.this);
      capacityEvictionSink.add(context);
    }

    public void notifyElementExpired(K key, V v) {
      notifyElementEvicted(key, v);
    }

    public void notifySizeChanged(L1ServerMapLocalCacheStore store) {
      //
    }

  }

  private void throwAssert(String msg) {
    throw new AssertionError(msg);
  }

  // ----------------------------------------
  // TCObjectSelfStore methods
  // ----------------------------------------

  public Object getById(ObjectID oid) {
    tcObjectStoreLock.readLock().lock();
    try {
      TCObjectSelf self = tcObjectSelfTempCache.get(oid);
      if (self != null) { return self; }

      if (!tcObjectSelfStoreOids.contains(oid)) {
        if (logger.isDebugEnabled()) {
          logger.debug("XXX GetById failed at TCObjectSelfStoreIDs, ObjectID=" + oid);
        }
        return null;
      }

      for (L1ServerMapLocalCacheStore store : this.stores.keySet()) {
        Object object = store.get(oid);
        if (object == null) {
          continue;
        }
        if (object instanceof TCObjectSelfStoreValue) {
          Object rv = ((TCObjectSelfStoreValue) object).getTCObjectSelf();
          initializeTCObjectSelfIfRequired(rv);
          return self;
        } else if (object instanceof List) {
          // for eventual value invalidation, use any of them to look up the value
          List list = (List) object;
          if (list.size() <= 0) {
            // all keys have been invalidated already, return null (lookup will happen)
            // we should wait until the server has been notified that the object id is not present
            waitUntilObjectIDAbsent(oid);
            if (logger.isDebugEnabled()) {
              logger.debug("XXX GetById failed when it got empty List, ObjectID=" + oid);
            }
            return null;
          }
          AbstractLocalCacheStoreValue localCacheStoreValue = (AbstractLocalCacheStoreValue) store.get(list.get(0));

          if (localCacheStoreValue == null) {
            waitUntilObjectIDAbsent(oid);
          }

          Object rv = localCacheStoreValue == null ? null : localCacheStoreValue.asEventualValue().getValue();
          initializeTCObjectSelfIfRequired(rv);

          if (rv == null && logger.isDebugEnabled()) {
            logger.debug("XXX GetById failed when localCacheStoreValue was null for eventual, ObjectID=" + oid);
          }

          return rv;
        } else {
          throw new AssertionError("Unknown type mapped to oid: " + oid + ", value: " + object
                                   + ". Expected to be mapped to either of TCObjectSelfStoreValue or a List");
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("XXX GetById failed when it couldn't find in any stores, ObjectID=" + oid);
      }
      return null;
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  private void initializeTCObjectSelfIfRequired(Object rv) {
    if (rv != null && rv instanceof TCObjectSelf) {
      TCObjectSelf self = (TCObjectSelf) rv;
      tcObjectSelfRemovedFromStoreCallback.initializeTCClazzIfRequired(self);
    }
  }

  private void waitUntilObjectIDAbsent(ObjectID oid) {
    tcObjectStoreLock.readLock().unlock();

    boolean isInterrupted = false;
    try {
      while (tcObjectSelfStoreOids.contains(oid)) {
        try {
          // since i know I am going to wait, let me wait on client lock manager instead of this condition
          synchronized (this.tcObjectSelfRemovedFromStoreCallback) {
            this.tcObjectSelfRemovedFromStoreCallback.wait(1000);
          }
        } catch (InterruptedException e) {
          isInterrupted = true;
        }
      }

    } finally {
      if (isInterrupted) {
        Thread.currentThread().interrupt();
      }
    }

    tcObjectStoreLock.readLock().lock();
  }

  public Object getByIdFromStore(ObjectID oid, L1ServerMapLocalCacheStore store) {
    tcObjectStoreLock.readLock().lock();
    try {
      TCObjectSelf self = tcObjectSelfTempCache.get(oid);
      if (self != null) { return self; }

      Object object = store.get(oid);
      if (object == null) { return null; }
      if (object instanceof TCObjectSelfStoreValue) {
        return ((TCObjectSelfStoreValue) object).getTCObjectSelf();
      } else if (object instanceof List) {
        // for eventual value invalidation, use any of them to look up the value
        List list = (List) object;
        if (list.size() <= 0) {
          // all keys have been invalidated already, return null (lookup will happen)
          return null;
        }
        AbstractLocalCacheStoreValue localCacheStoreValue = (AbstractLocalCacheStoreValue) store.get(list.get(0));
        return localCacheStoreValue == null ? null : localCacheStoreValue.asEventualValue().getValue();
      } else {
        throw new AssertionError("Unknown type mapped to oid: " + oid + ", value: " + object
                                 + ". Expected to be mapped to either of TCObjectSelfStoreValue or a List");
      }
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public void addTCObjectSelf(TCObjectSelf tcObjectSelf) {
    tcObjectStoreLock.writeLock().lock();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("XXX Adding TCObjectSelf to temp cache, ObjectID=" + tcObjectSelf.getObjectID());
      }
      this.tcObjectSelfTempCache.put(tcObjectSelf.getObjectID(), tcObjectSelf);
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  public void addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                              Object tcoself) {
    tcObjectStoreLock.writeLock().lock();
    try {
      if (tcoself instanceof TCObject) {
        // no need of instanceof check if tcoself is declared as TCObject only... skipping for tests.. refactor later
        ObjectID oid = ((TCObject) tcoself).getObjectID();
        if (logger.isDebugEnabled()) {
          logger.debug("XXX Adding TCObjectSelf to Store if necessary, ObjectID=" + oid);
        }

        tcObjectSelfStoreOids.add(oid);
        tcObjectSelfStoreSize.incrementAndGet();
        if (!localStoreValue.isEventualConsistentValue()) {
          store.put(((TCObject) tcoself).getObjectID(), new TCObjectSelfWrapper(tcoself),
                    PutType.PINNED_NO_SIZE_INCREMENT);
        } // else no need to store another mapping as for eventual already oid->localCacheEventualValue mapping exists,
        // and actual value is present in the localCacheEventualValue
      }
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  public void removeTCObjectSelfTemp(TCObjectSelf objectSelf, boolean notifyServer) {
    if (objectSelf == null) { return; }

    if (notifyServer) {
      if (logger.isDebugEnabled()) {
        logger.debug("XXX Removing TCObjectSelf from temp cache, ObjectID=" + objectSelf.getObjectID());
      }

      tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore(objectSelf);
    }

    // Tiny race left to resolve here ...
    tcObjectStoreLock.writeLock().lock();
    try {
      tcObjectSelfTempCache.remove(objectSelf.getObjectID());
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  /**
   * TODO: Re-write this method, its a mess right now
   */
  private void removeTCObjectSelfForId(ServerMapLocalCache serverMapLocalCache,
                                       AbstractLocalCacheStoreValue localStoreValue) {
    Object removed = null;
    ObjectID valueOid = localStoreValue.getObjectId();
    tcObjectStoreLock.writeLock().lock();
    try {
      tcObjectSelfTempCache.remove(valueOid);

      if (logger.isDebugEnabled()) {
        logger.debug("XXX Removing TCObjectSelf from Store, ObjectID=" + valueOid
                     + " , TCObjectSelfStore contains it = " + tcObjectSelfStoreOids.contains(valueOid));
      }

      if (ObjectID.NULL_ID.equals(valueOid) || !tcObjectSelfStoreOids.contains(valueOid)) { return; }

      // some asertions... can be removed?
      Object object = serverMapLocalCache.getInternalStore().get(valueOid);
      if (localStoreValue.isEventualConsistentValue()) {
        assertEventualIdMappingValue(valueOid, object);
        removed = localStoreValue.asEventualValue().getValue();
      } else {
        if (object != null) {
          if (!(object instanceof TCObjectSelfStoreValue)) {
            //
            throw new AssertionError("Object mapped by oid is not TCObjectSelfStoreValue, oid: " + valueOid
                                     + ", value: " + object);
          }
          removed = serverMapLocalCache.getInternalStore().remove(valueOid, RemoveType.NO_SIZE_DECREMENT);
          removed = ((TCObjectSelfStoreValue) removed).getTCObjectSelf();
        }
      }

    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }

    // TODO: remove the cast to TCObjectSelf, right now done to appease unit tests
    // to avoid deadlock, do this outside lock
    if (removed != null && removed instanceof TCObjectSelf) {
      this.tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore((TCObjectSelf) removed);
    }

    tcObjectStoreLock.writeLock().lock();
    try {
      tcObjectSelfStoreOids.remove(valueOid);
      tcObjectSelfStoreSize.decrementAndGet();
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }

    signalAll();
  }

  private void assertEventualIdMappingValue(ObjectID valueOid, Object object) throws AssertionError {
    if (object != null) {
      if (!(object instanceof List)) {
        //
        throw new AssertionError("With eventual, oid's can be mapped to List only, oid: " + valueOid + ", mapped to: "
                                 + object);
      } else {
        List list = (List) object;
        if (list.size() > 1) { throw new AssertionError(
                                                        "With eventual, oid's should be mapped to maximum of one key, oid: "
                                                            + valueOid + ", list: " + list); }
      }
    }
  }

  private void signalAll() {
    synchronized (this.tcObjectSelfRemovedFromStoreCallback) {
      this.tcObjectSelfRemovedFromStoreCallback.notifyAll();
    }
  }

  public int size() {
    tcObjectStoreLock.readLock().lock();
    try {
      return tcObjectSelfStoreSize.get();
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public void addAllObjectIDs(Set oids) {
    tcObjectStoreLock.readLock().lock();
    try {
      oids.addAll(this.tcObjectSelfStoreOids);
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public boolean contains(ObjectID objectID) {
    tcObjectStoreLock.readLock().lock();
    try {
      return this.tcObjectSelfTempCache.containsKey(objectID) || this.tcObjectSelfStoreOids.contains(objectID);
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  private class RemoveCallback implements ServerMapLocalCacheRemoveCallback {
    public void removedElement(Object key, AbstractLocalCacheStoreValue localStoreValue) {
      // clear the oid->value mapping from the tcoSelfStore
      ServerMapLocalCache serverMapLocalCache = localCaches.get(localStoreValue.getMapID());
      if (serverMapLocalCache == null) { throw new AssertionError("No local cache mapped for mapId: "
                                                                  + localStoreValue.getMapID()); }
      removeTCObjectSelfForId(serverMapLocalCache, localStoreValue);
    }
  }
}
