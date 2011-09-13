/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheLockProvider;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalStoreTransactionCompletionListener.TransactionCompleteOperation;
import com.tc.object.servermap.localcache.impl.LocalStoreKeySet.LocalStoreKeySetFilter;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ServerMapLocalCacheImpl implements ServerMapLocalCache {
  private static final TCLogger                                                     LOGGER                                                = TCLogging
                                                                                                                                              .getLogger(ServerMapLocalCacheImpl.class);
  private static final long                                                         SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS = TCPropertiesImpl
                                                                                                                                              .getProperties()
                                                                                                                                              .getLong(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_INCOHERENT_READ_TIMEOUT);

  private static final LocalStoreKeySetFilter                                       IGNORE_ID_FILTER                                      = new IgnoreIdsFilter();
  private static final Object                                                       NULL_VALUE                                            = new Object();

  private final L1ServerMapLocalCacheManager                                        globalLocalCacheManager;
  private volatile boolean                                                          localCacheEnabled;
  private volatile L1ServerMapLocalCacheStore<Object, AbstractLocalCacheStoreValue> localStore;
  private volatile L1ServerMapLocalCacheLockProvider                                lockProvider;
  private final ClientObjectManager                                                 objectManager;
  private final Manager                                                             manager;
  private final ServerMapLocalCacheRemoveCallback                                   removeCallback;

  private final Map<ObjectID, Object>                                               transactionsInProgressObjectIDs                       = new ConcurrentHashMap<ObjectID, Object>();
  private final Map<ObjectID, Object>                                               removedObjectIDs                                      = new ConcurrentHashMap<ObjectID, Object>();

  /**
   * Not public constructor, should be created only by the global local cache manager
   */
  ServerMapLocalCacheImpl(ClientObjectManager objectManager, Manager manager,
                          L1ServerMapLocalCacheManager globalLocalCacheManager, boolean islocalCacheEnbaled,
                          ServerMapLocalCacheRemoveCallback removeCallback,
                          L1ServerMapLocalCacheStore<Object, AbstractLocalCacheStoreValue> localStore) {
    this.objectManager = objectManager;
    this.manager = manager;
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.localCacheEnabled = islocalCacheEnbaled;
    this.removeCallback = removeCallback;
    this.localStore = localStore;
    this.lockProvider = this.localStore.getLockProvider();
  }

  public L1ServerMapLocalCacheStore getInternalStore() {
    return localStore;
  }

  private boolean isStoreInitialized() {
    if (localStore == null) {
      LOGGER.warn("Store yet not initialized");
      return false;
    }
    return true;
  }

  public void setLocalCacheEnabled(boolean enable) {
    this.localCacheEnabled = enable;
  }

  private void registerTransactionCompleteListener(final L1ServerMapLocalStoreTransactionCompletionListener listener) {
    if (listener == null) { throw new AssertionError("Listener cannot be null"); }
    ClientTransaction txn = this.objectManager.getTransactionManager().getCurrentTransaction();
    if (txn == null) { throw new UnlockedSharedObjectException(
                                                               "Attempt to access a shared object outside the scope of a shared lock.",
                                                               Thread.currentThread().getName(), manager.getClientID()); }
    txn.addTransactionCompleteListener(listener);
  }

  // TODO::FIXME:: There is a race for puts for same key from same vm - it races between the map.put() and
  // serverMapManager.put()
  public void addToCache(final Object key, final AbstractLocalCacheStoreValue localCacheValue,
                         final MapOperationType mapOperation) {
    if (!localCacheEnabled && !mapOperation.isMutateOperation()) {
      // local cache NOT enabled AND NOT a mutate operation, do not cache anything locally
      // for mutate ops keep in local cache till txn is complete
      return;
    }

    if (localCacheValue.isStrongConsistentValue()) {
      // Before putting we should remember the mapId for the lock Id as upon recall need to flush from these maps
      // (TODO: can lockId be potentially used by multiple maps?)
      globalLocalCacheManager.rememberMapIdForValueLockId(localCacheValue.asStrongValue().getLockId(), this);
    }

    addIdToKeysMappingIfNecessary(localCacheValue, key);

    { // scoping 'old' variable
      final AbstractLocalCacheStoreValue old;
      if (mapOperation.isMutateOperation()) {
        // put a pinned entry for mutate ops, unpinned on txn complete
        transactionStarted(localCacheValue.getObjectId());
        old = this.localStore.put(key, localCacheValue, PutType.PINNED);
      } else {
        old = this.localStore.put(key, localCacheValue, PutType.NORMAL);
      }

      // TODO: if ignoring when old is not null, make sure if we have removed a pinned entry we pin it back
      if (old != null && !old.getObjectId().equals(localCacheValue.getObjectId())) {
        if (!localCacheValue.isStrongConsistentValue()) {
          // remove id->keys mapping if not strong value. The new id can be different from the old one
          removeIdToKeysMappingIfNecessary(old, key);
        }
        // no need to remove id->keys mapping, as same key going to be added again
        // but need to remove the tcoSelf
        entryRemovedCallback(key, old);
      }
    }

    // register for transaction complete if mutate operation
    if (mapOperation.isMutateOperation()) {
      L1ServerMapLocalStoreTransactionCompletionListener listener = getTransactionCompleteListener(key,
                                                                                                   localCacheValue,
                                                                                                   mapOperation);
      if (listener == null) { throw new AssertionError("Transaction Complete Listener cannot be null for mutate ops"); }
      registerTransactionCompleteListener(listener);
    }
  }

  private void removeIdToKeysMappingIfNecessary(final AbstractLocalCacheStoreValue localCacheValue, final Object key) {
    if (!isStoreInitialized()) { return; }

    if (localCacheValue != null) {
      if (localCacheValue.getId() != null && localCacheValue.getId() != ObjectID.NULL_ID) {

        executeUnderSegmentWriteLock(localCacheValue.getId(), key, localCacheValue, RemoveIdKeyMappingCallback.INSTANCE);
      } // else: we don't add for null_id
    }
  }

  private void addIdToKeysMappingIfNecessary(final AbstractLocalCacheStoreValue localCacheValue, final Object key) {
    if (isStoreInitialized() && localCacheValue.getId() != null && localCacheValue.getId() != ObjectID.NULL_ID) {
      // we don't add for null_id
      executeUnderSegmentWriteLock(localCacheValue.getId(), key, AddIdKeyMappingCallback.INSTANCE);
    }
  }

  private L1ServerMapLocalStoreTransactionCompletionListener getTransactionCompleteListener(final Object key,
                                                                                            AbstractLocalCacheStoreValue value,
                                                                                            MapOperationType mapOperation) {
    if (!mapOperation.isMutateOperation()) {
      // no listener required for non mutate ops
      return null;
    }
    final L1ServerMapLocalStoreTransactionCompletionListener txnCompleteListener;
    if (localCacheEnabled) {
      // when local cache is enabled, remove the cached value if the operation is a REMOVE, otherwise just unpin
      TransactionCompleteOperation onTransactionComplete = mapOperation.isRemoveOperation() ? TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY
          : TransactionCompleteOperation.UNPIN_ENTRY;
      txnCompleteListener = new L1ServerMapLocalStoreTransactionCompletionListener(this, key, value,
                                                                                   onTransactionComplete);
    } else {
      // when local cache is disabled, always remove the cached value on txn complete
      txnCompleteListener = new L1ServerMapLocalStoreTransactionCompletionListener(
                                                                                   this,
                                                                                   key,
                                                                                   value,
                                                                                   TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY);
    }
    return txnCompleteListener;
  }

  // private boolean isExpired(final CachedItem ci, final int now) {
  // final ExpirableEntry ee;
  // if ((TCObjectServerMapImpl.this.tti <= 0 && TCObjectServerMapImpl.this.ttl <= 0)
  // || (ee = ci.getExpirableEntry()) == null) { return false; }
  // return now >= ee.expiresAt(TCObjectServerMapImpl.this.tti, TCObjectServerMapImpl.this.ttl);
  // }

  /**
   * TODO: this is a very bad implementation, we need to make this better in future
   */
  public void clear() {
    if (!isStoreInitialized()) { return; }

    Set keySet = this.localStore.getKeySet();
    for (Object key : keySet) {
      if (!isMetaInfoMapping(key)) {
        removeFromLocalCache(key);
      }
    }
  }

  public void clearInline() {
    if (!isStoreInitialized()) { return; }

    Set<LockID> lockIDs = new HashSet<LockID>();
    Set keySet = this.localStore.getKeySet();
    for (Object key : keySet) {
      if (!isMetaInfoMapping(key)) {
        Object value = localStore.remove(key, RemoveType.NORMAL);
        if (value != null && value instanceof AbstractLocalCacheStoreValue) {
          AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) value;
          LockID lockID = null;
          if (localValue.getId() != null) {
            // no need to attempt to remove the key again, as its already been removed on eviction notification
            lockID = executeUnderSegmentWriteLock(localValue.getId(), key, value,
                                                  RemoveEntryForKeyCallback.REMOVE_ONLY_META_MAPPING_CALLBACK_INSTANCE);
            if (lockID != null) {
              lockIDs.add(lockID);
            }
          }
          entryRemovedCallback(key, value);
        }
      }
    }
    recallLocksInline(lockIDs);
  }

  public int size() {
    if (!isStoreInitialized()) { return 0; }

    return this.localStore.size();
  }

  /**
   * When a remove from local cache is called, remove and flush
   */
  public void removeFromLocalCache(Object key) {
    if (!isStoreInitialized()) { return; }

    Object value = localStore.remove(key, RemoveType.NORMAL);
    if (value != null && value instanceof AbstractLocalCacheStoreValue) {
      AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) value;
      evictedFromStore(localValue.getId(), key, localValue);
    }
  }

  public void evictedFromStore(Object id, Object key, AbstractLocalCacheStoreValue value) {
    if (!isStoreInitialized()) { return; }

    LockID lockID = null;
    if (id != null) {
      // no need to attempt to remove the key again, as its already been removed on eviction notification
      lockID = executeUnderSegmentWriteLock(id, key, value,
                                            RemoveEntryForKeyCallback.REMOVE_ONLY_META_MAPPING_CALLBACK_INSTANCE);
    }
    entryRemovedCallback(key, value);
    initiateLockRecall(lockID);
  }

  public Set getKeySet() {
    return Collections
        .unmodifiableSet(new LocalStoreKeySet(localStore.getKeySet(), localStore.size(), IGNORE_ID_FILTER));
  }

  public Object getKeyOrValueForObjectID(ObjectID oid) {
    return executeUnderSegmentReadLock(oid, GetKeyOrValueForObjectIDCallback.GET_KEY_OR_VALUE_FOR_OBJECTID_INSTANCE);
  }

  /**
   * Returned value may be coherent or incoherent or null
   */
  public AbstractLocalCacheStoreValue getLocalValue(final Object key) {
    AbstractLocalCacheStoreValue value = this.localStore.get(key);
    if (value != null && value.isIncoherentValue() && isIncoherentTooLong(value)) {
      // if incoherent and been incoherent too long, remove from cache/map
      this.localStore.remove(key, RemoveType.NORMAL);
      entryRemovedCallback(key, value);
      return null;
    }
    if (value != null && isTransactionInProgressFor(value.getObjectId())) {
      return value.clone();
    } else return value;
  }

  public AbstractLocalCacheStoreValue getValue(final Object key) {
    return this.localStore.get(key);
  }

  private boolean isIncoherentTooLong(AbstractLocalCacheStoreValue value) {
    long lastCoherentTime = value.asIncoherentValue().getLastCoherentTime();
    return TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - lastCoherentTime)) >= SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS;
  }

  /**
   * Returned value is always coherent or null.
   */
  public AbstractLocalCacheStoreValue getCoherentLocalValue(final Object key) {
    final AbstractLocalCacheStoreValue value = getLocalValue(key);
    if (value != null && value.isIncoherentValue()) {
      // don't return incoherent items from here
      this.localStore.remove(key, RemoveType.NORMAL);
      entryRemovedCallback(key, value);
      return null;
    }
    return value;
  }

  public void unpinEntry(Object key, AbstractLocalCacheStoreValue value) {
    this.localStore.unpinEntry(key, value);
  }

  public void removeEntriesForObjectId(ObjectID objectId) {
    removeEntriesForId(objectId);
  }

  public void removeEntriesForLockId(LockID lockId) {
    removeEntriesForId(lockId);
  }

  private void removeEntriesForId(Object id) {
    if (!isStoreInitialized()) { return; }

    // This should be called when a lock has already been recalled, so shouldn't be a problem
    List list = executeUnderSegmentWriteLock(id, null, RemoveEntriesForIdCallback.INSTANCE);
    if (list != null) {
      for (Object key : list) {
        // remove each key from the backing map/store
        Object removed = localStore.remove(key, RemoveType.NORMAL);
        entryRemovedCallback(key, removed);
      }
    }
  }

  private void initiateLockRecall(LockID id) {
    if (id != null) {
      Set<LockID> lockID = Collections.singleton(id);
      globalLocalCacheManager.recallLocks(lockID);
    }
  }

  private void recallLocksInline(Set<LockID> lockIds) {
    globalLocalCacheManager.recallLocksInline(lockIds);
  }

  /**
   * used for tests
   */
  L1ServerMapLocalCacheStore getL1ServerMapLocalCacheStore() {
    return localStore;
  }

  private <V> V executeUnderSegmentReadLock(final Object id, final ExecuteUnderLockCallback<V> callback) {
    ReentrantReadWriteLock lock = this.lockProvider.getLock(id);
    try {
      lock.readLock().lock();
      return callback.callback(id, null, null, this.localStore, this.removeCallback);
    } finally {
      lock.readLock().unlock();
    }
  }

  private <V> V executeUnderSegmentWriteLock(final Object id, final Object key, Object value,
                                             final ExecuteUnderLockCallback<V> callback) {
    ReentrantReadWriteLock lock = this.lockProvider.getLock(id);
    try {
      lock.writeLock().lock();
      return callback.callback(id, key, value, this.localStore, this.removeCallback);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private <V> V executeUnderSegmentWriteLock(final Object id, final Object key,
                                             final ExecuteUnderLockCallback<V> callback) {
    ReentrantReadWriteLock lock = this.lockProvider.getLock(id);
    try {
      lock.writeLock().lock();
      return callback.callback(id, key, null, this.localStore, this.removeCallback);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private static interface ExecuteUnderLockCallback<V> {
    V callback(Object id, Object key, Object value, L1ServerMapLocalCacheStore backingMap,
               ServerMapLocalCacheRemoveCallback removeCallback);
  }

  private static class AddIdKeyMappingCallback implements ExecuteUnderLockCallback<Void> {

    public static AddIdKeyMappingCallback INSTANCE = new AddIdKeyMappingCallback();

    public Void callback(Object id, Object key, Object unused, L1ServerMapLocalCacheStore backingMap,
                         ServerMapLocalCacheRemoveCallback removeCallback) {
      List list;
      list = (List) backingMap.get(id);
      if (list == null) {
        list = new ArrayList();
      }

      if (!list.contains(key)) {
        list.add(key);
      }

      // add the list back
      backingMap.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
      return null;
    }
  }

  private static class RemoveIdKeyMappingCallback implements ExecuteUnderLockCallback<LockID> {
    public static RemoveIdKeyMappingCallback INSTANCE = new RemoveIdKeyMappingCallback();

    public LockID callback(Object id, Object key, Object removed, L1ServerMapLocalCacheStore backingMap,
                           ServerMapLocalCacheRemoveCallback removeCallback) {
      List list = (List) backingMap.get(id);
      if (list == null) { return null; }
      list.remove(key);

      // put back or remove the list
      if (list.size() == 0) {
        backingMap.remove(id, RemoveType.NO_SIZE_DECREMENT);
      } else {
        backingMap.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
      }

      return list.size() == 0 && (id instanceof LockID) ? (LockID) id : null;
    }
  }

  private static class RemoveEntriesForIdCallback implements ExecuteUnderLockCallback<List> {
    public static RemoveEntriesForIdCallback INSTANCE = new RemoveEntriesForIdCallback();

    public List callback(Object id, Object unusedParam, Object unusedParam2, L1ServerMapLocalCacheStore backingMap,
                         ServerMapLocalCacheRemoveCallback removeCallback) {
      // remove the list
      return (List) backingMap.remove(id, RemoveType.NO_SIZE_DECREMENT);
    }
  }

  private static class RemoveEntryForKeyCallback implements ExecuteUnderLockCallback<LockID> {
    public static RemoveEntryForKeyCallback REMOVE_ONLY_META_MAPPING_CALLBACK_INSTANCE = new RemoveEntryForKeyCallback();

    public LockID callback(Object id, Object key, Object removed, L1ServerMapLocalCacheStore backingMap,
                           ServerMapLocalCacheRemoveCallback removeCallback) {
      List list = (List) backingMap.get(id);
      if (list != null) {
        // remove the key from the id->list(keys)
        list.remove(key);
        // put back or remove the list
        if (list.size() == 0) {
          backingMap.remove(id, RemoveType.NO_SIZE_DECREMENT);
        } else {
          backingMap.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
        }
      }
      return list != null && list.size() == 0 && (id instanceof LockID) ? (LockID) id : null;
    }
  }

  private static class GetKeyOrValueForObjectIDCallback implements ExecuteUnderLockCallback {
    public static GetKeyOrValueForObjectIDCallback GET_KEY_OR_VALUE_FOR_OBJECTID_INSTANCE = new GetKeyOrValueForObjectIDCallback();

    public Object callback(Object id, Object key, Object value, L1ServerMapLocalCacheStore backingMap,
                           ServerMapLocalCacheRemoveCallback removeCallback) {
      Object object = backingMap.get(id);
      if (object instanceof List) {
        List list = (List) object;
        if (list.size() > 0) { return list.get(0); }
        return null;
      } else {
        return object;
      }
    }

  }

  private void entryRemovedCallback(Object key, Object removed) {
    if (removed != null) {
      // removed has to be of type AbstractLocalCacheStoreValue
      if (!(removed instanceof AbstractLocalCacheStoreValue)) {
        // throw new AssertionError("Key was mapped to different type other than AbstractLocalCacheStoreValue, key: "
        // + key + ", value: " + removed);

        // Removing Assertion for the time being until we fix L1 Cache manager logic

        return;
      }
      ObjectID objectID = ((AbstractLocalCacheStoreValue) removed).getObjectId();
      if (isTransactionInProgressFor(objectID, true)) { return; }

      removeCallback.removedElement(key, (AbstractLocalCacheStoreValue) removed);
    }
  }

  private boolean isTransactionInProgressFor(ObjectID objectId) {
    return isTransactionInProgressFor(objectId, false);
  }

  private boolean isTransactionInProgressFor(ObjectID objectId, boolean removedRequired) {
    if (ObjectID.NULL_ID.equals(objectId)) { return false; }

    ReentrantReadWriteLock lock = lockProvider.getLock(objectId);
    lock.readLock().lock();
    try {
      if (transactionsInProgressObjectIDs.containsKey(objectId)) {
        if (removedRequired) {
          removedObjectIDs.put(objectId, NULL_VALUE);
        }
        return true;
      } else {
        return false;
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  public void addToSink(L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    globalLocalCacheManager.transactionComplete(l1ServerMapLocalStoreTransactionCompletionListener);
  }

  public void transactionComplete(Object key,
                                  AbstractLocalCacheStoreValue value,
                                  L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    final ObjectID objectId = value.getObjectId();
    if (!ObjectID.NULL_ID.equals(objectId)) {
      Object doRemove;
      ReentrantReadWriteLock lock = lockProvider.getLock(objectId);
      lock.writeLock().lock();
      try {
        transactionsInProgressObjectIDs.remove(objectId);
        doRemove = removedObjectIDs.remove(objectId);
      } finally {
        lock.writeLock().unlock();
      }

      if (doRemove != null) {
        entryRemovedCallback(key, value);
      }
    }
  }

  private void transactionStarted(ObjectID objectId) {
    if (ObjectID.NULL_ID.equals(objectId)) { return; }

    ReentrantReadWriteLock lock = lockProvider.getLock(objectId);
    lock.writeLock().lock();
    try {
      transactionsInProgressObjectIDs.put(objectId, NULL_VALUE);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private static boolean isMetaInfoMapping(Object key) {
    return (key instanceof ObjectID || key instanceof LockID);
  }

  static class IgnoreIdsFilter implements LocalStoreKeySetFilter {

    public boolean accept(Object value) {
      if (isMetaInfoMapping(value)) { return false; }
      return true;
    }

  }

  public long onHeapSizeInBytes() {
    return this.localStore.onHeapSizeInBytes();
  }

  public long offHeapSizeInBytes() {
    return this.localStore.offHeapSizeInBytes();
  }

  public int onHeapSize() {
    return this.localStore.onHeapSize();
  }

  public int offHeapSize() {
    return this.localStore.offHeapSize();
  }

  public boolean containsKeyOnHeap(Object key) {
    return this.localStore.containsKeyOnHeap(key);
  }

  public boolean containsKeyOffHeap(Object key) {
    // If the key -> AbstractLocalCacheStoreEntry is around, the key->value mapping it corresponds to must also be
    // around
    return this.localStore.containsKeyOffHeap(key);
  }
}
