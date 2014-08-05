/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.net.NodeID;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.PinnedEntryInvalidationListener;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.platform.PlatformService;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class L1ServerMapLocalCacheManagerImpl implements L1ServerMapLocalCacheManager {

  private static final boolean                                                   FAULT_INVALIDATED_PINNED_ENTRIES     = TCPropertiesImpl
                                                                                                                          .getProperties()
                                                                                                                          .getBoolean(TCPropertiesConsts.L1_SERVERMAPMANAGER_FAULT_INVALIDATED_PINNED_ENTRIES,
                                                                                                                                      true);
  /**
   * For invalidations
   */
  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache>                 mapIdTolocalCache                    = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  // We also need the reverse mapping to easily remove the server map from existence on disposal
  private final TCConcurrentMultiMap<ServerMapLocalCache, ObjectID>              localCacheToMapIds                   = new TCConcurrentMultiMap<ServerMapLocalCache, ObjectID>();

  /**
   * All local caches
   */
  private final ConcurrentHashMap<ServerMapLocalCache, PinnedEntryFaultCallback> localCacheToPinnedEntryFaultCallback = new ConcurrentHashMap<ServerMapLocalCache, PinnedEntryFaultCallback>();

  /**
   * Identity HashMap of all stores
   */
  private final IdentityHashMap<L1ServerMapLocalCacheStore, ServerMapLocalCache> localStores                          = new IdentityHashMap<L1ServerMapLocalCacheStore, ServerMapLocalCache>();

  private final AtomicBoolean                                                    shutdown                             = new AtomicBoolean();
  private final Sink                                                             capacityEvictionSink;
  private final Sink                                                             txnCompleteSink;
  private final RemoveCallback                                                   removeCallback;
  private final TCObjectSelfStore                                                tcObjectSelfStore;
  private final PinnedEntryInvalidationListener                                  pinnedEntryInvalidationListener;

  public L1ServerMapLocalCacheManagerImpl(LocksRecallService locksRecallHelper, Sink capacityEvictionSink,
                                          Sink txnCompleteSink, Sink pinnedEntryFaultSink) {
    this.capacityEvictionSink = capacityEvictionSink;
    removeCallback = new RemoveCallback();
    tcObjectSelfStore = new TCObjectSelfStoreImpl(localCacheToPinnedEntryFaultCallback);
    this.txnCompleteSink = txnCompleteSink;
    this.pinnedEntryInvalidationListener = new Listener(localCacheToPinnedEntryFaultCallback, pinnedEntryFaultSink);
  }

  @Override
  public void cleanup() {
    // Clean-up of local cache is done by AggreagateServerMap on Rejoin Completed

    tcObjectSelfStore.cleanup();
    // all sinks will be cleaned-up as a part of stageManager.cleanAll() from ClientHandshakeManagerImpl.reset()
    // clientLockManager will be cleanup from clientHandshakeCallbacks
  }

  @Override
  public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
    this.tcObjectSelfStore.initializeTCObjectSelfStore(callback);
  }

  @Override
  public void initializeTCObjectSelfIfRequired(TCObjectSelf tcoSelf) {
    this.tcObjectSelfStore.initializeTCObjectSelfIfRequired(tcoSelf);
  }

  @Override
  public synchronized ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager,
                                                                PlatformService platformService,
                                                                boolean localCacheEnabled,
                                                                L1ServerMapLocalCacheStore serverMapLocalStore,
                                                                PinnedEntryFaultCallback callback) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }

    if (callback == null) { throw new AssertionError("PinnedEntryFault Callback is null"); }

    ServerMapLocalCache serverMapLocalCache = null;

    if (localStores.containsKey(serverMapLocalStore)) {
      serverMapLocalCache = localStores.get(serverMapLocalStore);
    } else {
      serverMapLocalCache = new ServerMapLocalCacheImpl(objectManager, platformService, this, localCacheEnabled,
                                                        removeCallback, serverMapLocalStore);

      if (FAULT_INVALIDATED_PINNED_ENTRIES) {
        serverMapLocalCache.registerPinnedEntryInvalidationListener(pinnedEntryInvalidationListener);
      }

      localStores.put(serverMapLocalStore, serverMapLocalCache);
      localCacheToPinnedEntryFaultCallback.put(serverMapLocalCache, callback);
      serverMapLocalStore.addListener(new L1ServerMapLocalCacheStoreListenerImpl(serverMapLocalCache));
    }

    if (!mapIdTolocalCache.containsKey(mapId)) {
      mapIdTolocalCache.put(mapId, serverMapLocalCache);
      localCacheToMapIds.add(serverMapLocalCache, mapId);
    }

    return serverMapLocalCache;
  }

  public void removeStore(L1ServerMapLocalCacheStore store) {
    ServerMapLocalCache localCache = localStores.remove(store);
    if (localCache != null) {
      localCacheToPinnedEntryFaultCallback.remove(localCache);
      for (ObjectID mapId : localCacheToMapIds.removeAll(localCache)) {
        mapIdTolocalCache.remove(mapId);
      }
    }
  }

  private void throwAlreadyShutdownException() {
    throw new TCRuntimeException("GlobalCacheManager is already shut down.");
  }

  // TODO: is this method needed?
  public void removeLocalCache(ObjectID mapID) {
    mapIdTolocalCache.remove(mapID);
  }

  @Override
  public ObjectIDSet getObjectIDsToValidate(final NodeID remoteNode) {
    ObjectIDSet validations = tcObjectSelfStore.getObjectIDsToValidate(remoteNode);
    for (ServerMapLocalCache cache : localStores.values()) {
      cache.handleObjectIDsToValidate(validations);
    }
    return validations;
  }

  /**
   * This method is called only for invalidations
   */
  @Override
  public ObjectIDSet removeEntriesForObjectId(ObjectID mapID, Set<ObjectID> set) {
    ObjectIDSet invalidationsFailed = new BitSetObjectIDSet();

    if (ObjectID.NULL_ID.equals(mapID)) {
      for (ServerMapLocalCache cache : localCacheToPinnedEntryFaultCallback.keySet()) {
        removeFromCache(set, invalidationsFailed, cache);
      }
    } else {
      ServerMapLocalCache cache = mapIdTolocalCache.get(mapID);
      if (cache != null) {
        removeFromCache(set, invalidationsFailed, cache);
      }
    }
    return invalidationsFailed;
  }

  private void removeFromCache(Set<ObjectID> set, ObjectIDSet invalidationsFailed, ServerMapLocalCache cache) {
    for (ObjectID id : set) {
      boolean success = cache.removeEntriesForObjectId(id);
      if (!success) {
        invalidationsFailed.add(id);
      }
    }
  }

  @Override
  public void evictElements(Map evictedElements, ServerMapLocalCache localCache) {
    Set<Map.Entry> entries = evictedElements.entrySet();
    for (Entry entry : entries) {
      localCache.entryEvicted(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void rejoinInProgress(boolean rejoinInProgress) {
    tcObjectSelfStore.rejoinInProgress(rejoinInProgress);
  }

  @Override
  public synchronized void shutdown(boolean fromShutdownHook) {
    tcObjectSelfStore.shutdown(fromShutdownHook);

    shutdown.set(true);
    if (!fromShutdownHook) {
      for (L1ServerMapLocalCacheStore store : localStores.keySet().toArray(new L1ServerMapLocalCacheStore[0])) {
        store.clear();
        store.dispose();
      }
    }
  }

  class L1ServerMapLocalCacheStoreListenerImpl<K, V> implements L1ServerMapLocalCacheStoreListener<K, V> {
    private final ServerMapLocalCache serverMapLocalCache;

    public L1ServerMapLocalCacheStoreListenerImpl(ServerMapLocalCache serverMapLocalCache) {
      this.serverMapLocalCache = serverMapLocalCache;
    }

    @Override
    public void notifyElementEvicted(K key, V value) {
      notifyElementsEvicted(Collections.singletonMap(key, value));
    }

    // TODO: does this need to be present in the interface? not called from outside
    @Override
    public void notifyElementsEvicted(Map<K, V> evictedElements) {
      // This should be inside another thread, if not it will cause a deadlock
      L1ServerMapEvictedElementsContext context = new L1ServerMapEvictedElementsContext(evictedElements,
                                                                                        serverMapLocalCache);
      capacityEvictionSink.add(context);
    }

    @Override
    public void notifyElementExpired(K key, V v) {
      notifyElementEvicted(key, v);
    }

    @Override
    public void notifyDisposed(L1ServerMapLocalCacheStore store) {
      removeStore(store);
    }
  }

  // ----------------------------------------
  // TCObjectSelfStore methods
  // ----------------------------------------

  @Override
  public Object getById(ObjectID oid) {
    return tcObjectSelfStore.getById(oid);
  }

  @Override
  public void addTCObjectSelfTemp(TCObjectSelf tcObjectSelf) {
    tcObjectSelfStore.addTCObjectSelfTemp(tcObjectSelf);
  }

  @Override
  public boolean addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                                 Object tcoself, boolean isNew) {
    return tcObjectSelfStore.addTCObjectSelf(store, localStoreValue, tcoself, isNew);
  }

  @Override
  public void removeTCObjectSelfTemp(TCObjectSelf objectSelf, boolean notifyServer) {
    tcObjectSelfStore.removeTCObjectSelfTemp(objectSelf, notifyServer);
  }

  @Override
  public void removeTCObjectSelf(AbstractLocalCacheStoreValue localStoreValue) {
    tcObjectSelfStore.removeTCObjectSelf(localStoreValue);
  }

  @Override
  public void removeTCObjectSelf(TCObjectSelf self) {
    tcObjectSelfStore.removeTCObjectSelf(self);
  }

  @Override
  public int size() {
    return tcObjectSelfStore.size();
  }

  @Override
  public void addAllObjectIDs(Set oids) {
    tcObjectSelfStore.addAllObjectIDs(oids);
  }

  @Override
  public boolean contains(ObjectID objectID) {
    return tcObjectSelfStore.contains(objectID);
  }

  @Override
  public void removeObjectById(ObjectID oid) {
    tcObjectSelfStore.removeObjectById(oid);
  }

  private class RemoveCallback implements ServerMapLocalCacheRemoveCallback {
    @Override
    public void removedElement(AbstractLocalCacheStoreValue localStoreValue) {
      removeTCObjectSelf(localStoreValue);
    }

    @Override
    public void removedElement(TCObjectSelf removed) {
      removeTCObjectSelf(removed);
    }
  }

  @Override
  public void transactionComplete(L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    txnCompleteSink.add(l1ServerMapLocalStoreTransactionCompletionListener);
  }

  private static class Listener implements PinnedEntryInvalidationListener {
    private final Sink                                               pinnedEntryFaultSink;
    private final Map<ServerMapLocalCache, PinnedEntryFaultCallback> localCacheToPinnedEntryFaultCallback;

    public Listener(Map<ServerMapLocalCache, PinnedEntryFaultCallback> localCacheToPinnedEntryFaultCallback,
                    Sink pinnedEntryFaultSink) {
      this.localCacheToPinnedEntryFaultCallback = localCacheToPinnedEntryFaultCallback;
      this.pinnedEntryFaultSink = pinnedEntryFaultSink;

    }

    @Override
    public void notifyKeyInvalidated(ServerMapLocalCache cache, Object key, boolean eventual) {
      PinnedEntryFaultCallback callback = localCacheToPinnedEntryFaultCallback.get(cache);
      pinnedEntryFaultSink.add(new PinnedEntryFaultContext(key, eventual, callback));
    }

  }

}
