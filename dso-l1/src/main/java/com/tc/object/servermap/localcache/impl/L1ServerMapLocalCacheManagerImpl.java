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
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class L1ServerMapLocalCacheManagerImpl implements L1ServerMapLocalCacheManager {
  private final Object                                                           NULL_VALUE              = new Object();

  private static final boolean                                                   PINNING_ENABLED         = TCPropertiesImpl
                                                                                                             .getProperties()
                                                                                                             .getBoolean(
                                                                                                                         TCPropertiesConsts.L1_LOCKMANAGER_PINNING_ENABLED);

  /**
   * For invalidations
   */
  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache>                 mapIdTolocalCache       = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  // We also need the reverse mapping to easily remove the server map from existence on disposal
  private final TCConcurrentMultiMap<ServerMapLocalCache, ObjectID>              localCacheToMapIds      = new TCConcurrentMultiMap<ServerMapLocalCache, ObjectID>();

  /**
   * For lock recalls
   */
  private final TCConcurrentMultiMap<LockID, ServerMapLocalCache>                lockIdsToLocalCache     = new TCConcurrentMultiMap<LockID, ServerMapLocalCache>();

  /**
   * All local caches
   */
  private final ConcurrentHashMap<ServerMapLocalCache, Object>                   localCaches             = new ConcurrentHashMap<ServerMapLocalCache, Object>();

  /**
   * Identity HashMap of all stores
   */
  private final IdentityHashMap<L1ServerMapLocalCacheStore, ServerMapLocalCache> localStores             = new IdentityHashMap<L1ServerMapLocalCacheStore, ServerMapLocalCache>();

  private final GlobalL1ServerMapLocalCacheStoreListener                         localCacheStoreListener = new GlobalL1ServerMapLocalCacheStoreListener();
  private final AtomicBoolean                                                    shutdown                = new AtomicBoolean();
  private final LocksRecallService                                               locksRecallHelper;
  private final Sink                                                             capacityEvictionSink;
  private final Sink                                                             txnCompleteSink;
  private final RemoveCallback                                                   removeCallback;
  private final TCObjectSelfStore                                                tcObjectSelfStore;
  private volatile ClientLockManager                                             lockManager;

  private static TCLogger                                                        logger                  = TCLogging
                                                                                                             .getLogger(L1ServerMapLocalCacheManagerImpl.class);

  public L1ServerMapLocalCacheManagerImpl(LocksRecallService locksRecallHelper, Sink capacityEvictionSink,
                                          Sink txnCompleteSink) {
    this.locksRecallHelper = locksRecallHelper;
    this.capacityEvictionSink = capacityEvictionSink;
    removeCallback = new RemoveCallback();
    tcObjectSelfStore = new TCObjectSelfStoreImpl(localCaches);
    this.txnCompleteSink = txnCompleteSink;
  }

  public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
    this.tcObjectSelfStore.initializeTCObjectSelfStore(callback);
  }

  public void setLockManager(ClientLockManager lockManager) {
    this.lockManager = lockManager;
  }

  public void initializeTCObjectSelfIfRequired(TCObjectSelf tcoSelf) {
    this.tcObjectSelfStore.initializeTCObjectSelfIfRequired(tcoSelf);
  }

  public synchronized ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager,
                                                                Manager manager, boolean localCacheEnabled,
                                                                L1ServerMapLocalCacheStore serverMapLocalStore) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }

    ServerMapLocalCache serverMapLocalCache = null;

    if (localStores.containsKey(serverMapLocalStore)) {
      serverMapLocalCache = localStores.get(serverMapLocalStore);
    } else {
      serverMapLocalCache = new ServerMapLocalCacheImpl(objectManager, manager, this, localCacheEnabled,
                                                        removeCallback, serverMapLocalStore);
      localStores.put(serverMapLocalStore, serverMapLocalCache);
      localCaches.put(serverMapLocalCache, NULL_VALUE);
      serverMapLocalStore.addListener(localCacheStoreListener);
    }

    if (!mapIdTolocalCache.containsKey(mapId)) {
      mapIdTolocalCache.put(mapId, serverMapLocalCache);
      localCacheToMapIds.add(serverMapLocalCache, mapId);
    }

    return serverMapLocalCache;
  }

  public void removeStore(L1ServerMapLocalCacheStore store) {
    ServerMapLocalCache localCache = localStores.remove(store);
    localCaches.remove(localCache);
    for (ObjectID mapId : localCacheToMapIds.removeAll(localCache)) {
      mapIdTolocalCache.remove(mapId);
    }
  }

  private void throwAlreadyShutdownException() {
    throw new TCRuntimeException("GlobalCacheManager is already shut down.");
  }

  // TODO: is this method needed?
  public void removeLocalCache(ObjectID mapID) {
    mapIdTolocalCache.remove(mapID);
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
    tcObjectSelfStore.addAllObjectIDsToValidate(invalidations);
  }

  /**
   * This method is called only for invalidations
   */
  public void removeEntriesForObjectId(ObjectID mapID, Set<ObjectID> set) {
    if (ObjectID.NULL_ID.equals(mapID)) {
      for (ServerMapLocalCache cache : localCaches.keySet()) {
        for (ObjectID id : set) {
          cache.removeEntriesForObjectId(id);
        }
      }
    } else {
      ServerMapLocalCache cache = mapIdTolocalCache.get(mapID);
      if (cache != null) {
        for (ObjectID id : set) {
          cache.removeEntriesForObjectId(id);
        }
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

    final Set<ServerMapLocalCache> caches = lockIdsToLocalCache.removeAll(lockID);

    for (ServerMapLocalCache localCache : caches) {
      localCache.removeEntriesForLockId(lockID);
    }
  }

  public void rememberMapIdForValueLockId(LockID valueLockId, ServerMapLocalCache localCache) {
    if (PINNING_ENABLED) {
      this.lockManager.pinLock(valueLockId);
    }

    lockIdsToLocalCache.add(valueLockId, localCache);
  }

  public void evictElements(Map evictedElements) {
    Set<Map.Entry> entries = evictedElements.entrySet();

    for (Entry entry : entries) {
      if (!(entry.getValue() instanceof AbstractLocalCacheStoreValue)) { throw new AssertionError(
                                                                                                  "Pinned elements should not be evicted, key="
                                                                                                      + entry
                                                                                                          .getValue()); }

      AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) entry.getValue();
      ObjectID mapID = value.getMapID();
      ServerMapLocalCache localCache = mapIdTolocalCache.get(mapID);
      if (localCache != null) {
        // the entry has been already removed from the local store, this will remove the id->key mapping if it exists
        localCache.evictedFromStore(value.getId(), entry.getKey(), value);
      } else {
        throwAssert("LocalCache not mapped for mapId: " + mapID);
      }
    }
  }

  public synchronized void shutdown() {
    shutdown.set(true);

    for (L1ServerMapLocalCacheStore store : localStores.keySet()) {
      store.clear();
    }
  }

  class GlobalL1ServerMapLocalCacheStoreListener<K, V> implements L1ServerMapLocalCacheStoreListener<K, V> {

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

    public void notifyDisposed(L1ServerMapLocalCacheStore store) {
      removeStore(store);
    }
  }

  private void throwAssert(String msg) {
    throw new AssertionError(msg);
  }

  // ----------------------------------------
  // TCObjectSelfStore methods
  // ----------------------------------------

  public Object getById(ObjectID oid) {
    return tcObjectSelfStore.getById(oid);
  }

  public Object getByIdFromCache(ObjectID oid, ServerMapLocalCache localCache) {
    return tcObjectSelfStore.getByIdFromCache(oid, localCache);
  }

  public void addTCObjectSelfTemp(TCObjectSelf tcObjectSelf) {
    tcObjectSelfStore.addTCObjectSelfTemp(tcObjectSelf);
  }

  public boolean addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                                 Object tcoself, boolean isNew) {
    return tcObjectSelfStore.addTCObjectSelf(store, localStoreValue, tcoself, isNew);
  }

  public void removeTCObjectSelfTemp(TCObjectSelf objectSelf, boolean notifyServer) {
    tcObjectSelfStore.removeTCObjectSelfTemp(objectSelf, notifyServer);
  }

  public void removeTCObjectSelf(AbstractLocalCacheStoreValue localStoreValue) {
    tcObjectSelfStore.removeTCObjectSelf(localStoreValue);
  }

  public void removeTCObjectSelf(ServerMapLocalCache serverMapLocalCache, AbstractLocalCacheStoreValue localStoreValue) {
    tcObjectSelfStore.removeTCObjectSelf(serverMapLocalCache, localStoreValue);
  }

  public int size() {
    return tcObjectSelfStore.size();
  }

  public void addAllObjectIDs(Set oids) {
    tcObjectSelfStore.addAllObjectIDs(oids);
  }

  public boolean contains(ObjectID objectID) {
    return tcObjectSelfStore.contains(objectID);
  }

  private class RemoveCallback implements ServerMapLocalCacheRemoveCallback {
    public void removedElement(Object key, AbstractLocalCacheStoreValue localStoreValue) {
      // clear the oid->value mapping from the tcoSelfStore
      ServerMapLocalCache serverMapLocalCache = mapIdTolocalCache.get(localStoreValue.getMapID());
      if (serverMapLocalCache == null) {
        logger.warn("No local cache mapped for mapId: " + localStoreValue.getMapID());
        removeTCObjectSelf(localStoreValue);
      } else {
        removeTCObjectSelf(serverMapLocalCache, localStoreValue);
      }
    }
  }

  public void transactionComplete(
                                  L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    txnCompleteSink.add(l1ServerMapLocalStoreTransactionCompletionListener);
  }
}
