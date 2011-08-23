/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.invalidation.Invalidations;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
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
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class L1ServerMapLocalCacheManagerImpl implements L1ServerMapLocalCacheManager {

  private static final boolean                                   PINNING_ENABLED         = TCPropertiesImpl
                                                                                             .getProperties()
                                                                                             .getBoolean(
                                                                                                         TCPropertiesConsts.L1_LOCKMANAGER_PINNING_ENABLED);

  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache> localCaches             = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  private final TCConcurrentMultiMap<LockID, ObjectID>           lockIdsToCdsmIds        = new TCConcurrentMultiMap<LockID, ObjectID>();
  private final GlobalL1ServerMapLocalCacheStoreListener         localCacheStoreListener = new GlobalL1ServerMapLocalCacheStoreListener();
  private final AtomicBoolean                                    shutdown                = new AtomicBoolean();
  private final LocksRecallService                               locksRecallHelper;
  private final Sink                                             capacityEvictionSink;
  private final RemoveCallback                                   removeCallback;
  private final TCObjectSelfStoreImpl                            tcObjectSelfStore;
  private volatile ClientLockManager                             lockManager;

  public L1ServerMapLocalCacheManagerImpl(LocksRecallService locksRecallHelper, Sink capacityEvictionSink) {
    this.locksRecallHelper = locksRecallHelper;
    this.capacityEvictionSink = capacityEvictionSink;
    removeCallback = new RemoveCallback();
    tcObjectSelfStore = new TCObjectSelfStoreImpl(localCaches);
  }

  public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
    this.tcObjectSelfStore.initializeTCObjectSelfStore(callback);
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

  public void addStoreListener(L1ServerMapLocalCacheStore store, ObjectID mapID) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }

    tcObjectSelfStore.addStoreListener(store, localCacheStoreListener, mapID);
  }

  public void removeStore(L1ServerMapLocalCacheStore store) {
    tcObjectSelfStore.removeStore(store);
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
      if (!(entry.getValue() instanceof AbstractLocalCacheStoreValue)) { throw new AssertionError(
                                                                                                  "Pinned elements should not be evicted, key="
                                                                                                      + entry
                                                                                                          .getValue()); }

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
    tcObjectSelfStore.clear();
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

    public void notifySizeChanged(L1ServerMapLocalCacheStore store) {
      //
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

  public Object getByIdFromStore(ObjectID oid, L1ServerMapLocalCacheStore store) {
    return tcObjectSelfStore.getByIdFromStore(oid, store);
  }

  public void addTCObjectSelfTemp(TCObjectSelf tcObjectSelf) {
    tcObjectSelfStore.addTCObjectSelfTemp(tcObjectSelf);
  }

  public void addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                              Object tcoself) {
    tcObjectSelfStore.addTCObjectSelf(store, localStoreValue, tcoself);
  }

  public void removeTCObjectSelfTemp(TCObjectSelf objectSelf, boolean notifyServer) {
    tcObjectSelfStore.removeTCObjectSelfTemp(objectSelf, notifyServer);
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
      ServerMapLocalCache serverMapLocalCache = localCaches.get(localStoreValue.getMapID());
      if (serverMapLocalCache == null) { throw new AssertionError("No local cache mapped for mapId: "
                                                                  + localStoreValue.getMapID()); }
      removeTCObjectSelf(serverMapLocalCache, localStoreValue);
    }
  }
}
