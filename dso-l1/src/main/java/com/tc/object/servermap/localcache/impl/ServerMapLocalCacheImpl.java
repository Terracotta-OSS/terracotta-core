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

/**
 * Eventual Mapping
 * <ul>
 * <li>key -> (value, valuoid, mapid)</li>
 * <li>valueoid -> key</li>
 * </ul>
 * <p/>
 * Strong Mapping (if value is not literal)
 * <ul>
 * <li>key -> (value, lockid, mapid)</li>
 * <li>valueoid -> key</li>
 * <li>lockid -> List&lt;valueoid></li>
 * </ul>
 * <p/>
 * Strong Mapping (if value is literal)
 * <ul>
 * <li>key -> (value, lockid, mapid)</li>
 * <li>lockid -> List&lt;key></li>
 * </ul>
 * <p/>
 * Bulk load
 * <ul>
 * <li>key -> (value, valueoid, mapid)</li>
 * <li>valueoid -> key</li>
 * </ul>
 */
public final class ServerMapLocalCacheImpl implements ServerMapLocalCache {
  private static final TCLogger                      LOGGER                                                = TCLogging
                                                                                                               .getLogger(ServerMapLocalCacheImpl.class);
  private static final long                          SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS = TCPropertiesImpl
                                                                                                               .getProperties()
                                                                                                               .getLong(
                                                                                                                        TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_INCOHERENT_READ_TIMEOUT);

  private static final LocalStoreKeySetFilter        IGNORE_ID_FILTER                                      = new IgnoreIdsFilter();
  private static final Object                        NULL_VALUE                                            = new Object();

  private final L1ServerMapLocalCacheManager         l1LocalCacheManager;
  private volatile boolean                           localCacheEnabled;
  private volatile L1ServerMapLocalCacheStore        localStore;
  private volatile L1ServerMapLocalCacheLockProvider lockProvider;
  private final ClientObjectManager                  objectManager;
  private final Manager                              manager;
  private final ServerMapLocalCacheRemoveCallback    removeCallback;

  private final Map<ObjectID, Object>                transactionsInProgressObjectIDs                       = new ConcurrentHashMap<ObjectID, Object>();
  private final Map<ObjectID, Object>                removedObjectIDs                                      = new ConcurrentHashMap<ObjectID, Object>();

  /**
   * Not public constructor, should be created only by the global local cache manager
   */
  ServerMapLocalCacheImpl(ClientObjectManager objectManager, Manager manager,
                          L1ServerMapLocalCacheManager globalLocalCacheManager, boolean islocalCacheEnbaled,
                          ServerMapLocalCacheRemoveCallback removeCallback, L1ServerMapLocalCacheStore localStore) {
    this.objectManager = objectManager;
    this.manager = manager;
    this.l1LocalCacheManager = globalLocalCacheManager;
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

  public void addToCache(final Object key, final Object actualValue,
                         final AbstractLocalCacheStoreValue localCacheValue, ObjectID valueObjectID,
                         final MapOperationType mapOperation) {
    if (!localCacheEnabled && !mapOperation.isMutateOperation()) {
      // local cache NOT enabled AND NOT a mutate operation, do not cache anything locally
      // for mutate ops keep in local cache till txn is complete
      return;
    }

    if (localCacheValue.isStrongConsistentValue()) {
      l1LocalCacheManager.rememberMapIdForValueLockId(localCacheValue.asStrongValue().getLockId(), this);
    }

    addIdForKeyMappingIfNecessary(localCacheValue, key, valueObjectID);

    { // scoping 'old' variable
      final AbstractLocalCacheStoreValue old;
      if (mapOperation.isMutateOperation()) {
        // put a pinned entry for mutate ops, unpinned on txn complete
        transactionStarted(valueObjectID);
        old = (AbstractLocalCacheStoreValue) this.localStore.put(key, localCacheValue, PutType.PINNED);
      } else {
        old = (AbstractLocalCacheStoreValue) this.localStore.put(key, localCacheValue, PutType.NORMAL);
      }

      if (old != null && !old.getValueObjectId().equals(valueObjectID)) {
        removeIdMappingForKeyIfNecessary(key, localCacheValue, old);
        // but need to remove the tcoSelf
        remoteRemoveObjectIfPossible(old);
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

  private void removeIdMappingForKeyIfNecessary(final Object key, final AbstractLocalCacheStoreValue localCacheValue,
                                                final AbstractLocalCacheStoreValue old) {
    if (localCacheValue.isStrongConsistentValue() && old.getValueObjectId().equals(ObjectID.NULL_ID)) {
      // remove from lock id mapping only when value is not literal.
      // don want to remove lockid -> key mapping
      // can remove from lockid -> valueid1, valueid2
      return;
    }
    removeIdForKeyMapping(old.getMetaId(), key, old);
  }

  private void addIdForKeyMappingIfNecessary(final AbstractLocalCacheStoreValue localCacheValue, final Object key,
                                             ObjectID valueObjectID) {
    if (!isStoreInitialized()) { return; }

    if (localCacheValue.getValueObject() != null) {
      addIdForKeyMapping(key, valueObjectID, localCacheValue);
    }
  }

  private void addIdForKeyMapping(Object key, ObjectID valueObjectID, AbstractLocalCacheStoreValue localCacheValue) {
    if (!valueObjectID.equals(ObjectID.NULL_ID)) {
      localStore.put(valueObjectID, key, PutType.PINNED_NO_SIZE_INCREMENT);
    }

    if (localCacheValue.isStrongConsistentValue()) {
      addLockIDMapping(localCacheValue.asStrongValue().getLockId(), key, valueObjectID);
    }
  }

  private void addLockIDMapping(LockID id, Object key, ObjectID valueObjectID) {
    ReentrantReadWriteLock lock = lockProvider.getLock(id);
    lock.writeLock().lock();
    try {
      List list;
      Object valueToPut = valueObjectID.equals(ObjectID.NULL_ID) ? key : valueObjectID;

      list = (List) localStore.get(id);
      if (list == null) {
        list = new ArrayList();
      }
      list.add(valueToPut);
      localStore.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public LockID removeIdForKeyMapping(Object id, Object key, AbstractLocalCacheStoreValue localCacheValue) {
    if (!isStoreInitialized()) { return null; }
    if (localCacheValue == null || localCacheValue.getValueObject() == null) { return null; }

    ObjectID valueObjectID = localCacheValue.getValueObjectId();
    if (!valueObjectID.equals(ObjectID.NULL_ID)) {
      localStore.remove(valueObjectID, RemoveType.NO_SIZE_DECREMENT);
    }

    if (localCacheValue.isStrongConsistentValue()) {
      return removeLockIDMapping((LockID) id, key, valueObjectID, localCacheValue);
    } else {
      return null;
    }
  }

  private LockID removeLockIDMapping(LockID id, Object key, ObjectID valueObjectID,
                                     AbstractLocalCacheStoreValue localCacheValue) {
    ReentrantReadWriteLock lock = lockProvider.getLock(id);
    lock.writeLock().lock();

    try {
      List list = (List) localStore.get(id);
      if (list == null) { return null; }

      if (valueObjectID.equals(ObjectID.NULL_ID)) {
        list.remove(key);
      } else {
        list.remove(valueObjectID);
      }

      // put back or remove the list
      if (list.size() == 0) {
        localStore.remove(id, RemoveType.NO_SIZE_DECREMENT);
        return id;
      } else {
        localStore.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
      }
      return null;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private L1ServerMapLocalStoreTransactionCompletionListener getTransactionCompleteListener(
                                                                                            final Object key,
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
        if (value instanceof AbstractLocalCacheStoreValue) {
          AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) value;
          LockID lockID = removeIdForKeyMapping(localValue.getMetaId(), key, localValue);
          if (lockID != null) {
            lockIDs.add(lockID);
          }
          remoteRemoveObjectIfPossible(localValue);
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
    if (value instanceof AbstractLocalCacheStoreValue) {
      AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) value;
      evictedFromStore(localValue.getMetaId(), key, localValue);
    }
  }

  public void removeFromLocalCache(Object key, AbstractLocalCacheStoreValue value) {
    if (!isStoreInitialized()) { return; }

    Object removedValue = localStore.remove(key, value, RemoveType.NORMAL);
    if (removedValue instanceof AbstractLocalCacheStoreValue) {
      AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) removedValue;
      evictedFromStore(localValue.getMetaId(), key, localValue);
    }
  }

  public void evictedFromStore(Object id, Object key, AbstractLocalCacheStoreValue value) {
    if (!isStoreInitialized()) { return; }

    LockID lockID = null;
    if (id != null) {
      // no need to attempt to remove the key again, as its already been removed on eviction notification
      lockID = removeIdForKeyMapping(id, key, value);
    }
    remoteRemoveObjectIfPossible(value);
    initiateLockRecall(lockID);
  }

  public Set getKeySet() {
    return Collections
        .unmodifiableSet(new LocalStoreKeySet(localStore.getKeySet(), localStore.size(), IGNORE_ID_FILTER));
  }

  public Object getValue(final Object key) {
    return this.localStore.get(key);
  }

  /**
   * Returned value may be coherent or incoherent or null
   */
  public AbstractLocalCacheStoreValue getLocalValue(final Object key) {
    AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) this.localStore.get(key);
    if (value != null && value.isIncoherentValue() && isIncoherentTooLong(value)) {
      // if incoherent and been incoherent too long, remove from cache/map
      removeFromLocalCache(key, value);
      return null;
    }
    return value;
  }

  /**
   * Returned value is always coherent or null.
   */
  public AbstractLocalCacheStoreValue getCoherentLocalValue(final Object key) {
    final AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) this.localStore.get(key);
    if (value != null && value.isIncoherentValue()) {
      // don't return incoherent items from here
      removeFromLocalCache(key, value);
      return null;
    }
    return value;
  }

  private boolean isIncoherentTooLong(AbstractLocalCacheStoreValue value) {
    long lastCoherentTime = value.asIncoherentValue().getLastCoherentTime();
    return TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - lastCoherentTime)) >= SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS;
  }

  public void unpinEntry(Object key, AbstractLocalCacheStoreValue value) {
    this.localStore.unpinEntry(key, value);
  }

  public boolean removeEntriesForObjectId(ObjectID objectId) {
    if (!isStoreInitialized()) { return true; }

    Object key = this.localStore.get(objectId);
    if (key == null) { return false; }

    Object value = localStore.get(key);
    if (value == null) { return false; }

    localStore.remove(objectId, RemoveType.NO_SIZE_DECREMENT);

    AbstractLocalCacheStoreValue actualValue = (AbstractLocalCacheStoreValue) value;
    if (actualValue.getValueObjectId().equals(objectId)) {
      Object removedValue = localStore.remove(key, value, RemoveType.NORMAL);
      remoteRemoveObjectIfPossible((AbstractLocalCacheStoreValue) removedValue);
    }

    return true;
  }

  public void removeEntriesForLockId(LockID lockId) {
    if (!isStoreInitialized()) { return; }

    Object object = this.localStore.remove(lockId, RemoveType.NO_SIZE_DECREMENT);
    if (object != null) {
      List list = (List) object;
      for (Object removed : list) {
        removeRemainingMappingsForLockID(removed);
      }
    }
  }

  private void removeRemainingMappingsForLockID(Object keyOrValueId) {
    Object key = null;
    if (keyOrValueId instanceof ObjectID) {
      key = localStore.remove(keyOrValueId, RemoveType.NO_SIZE_DECREMENT);
    }
    if (key != null) {
      Object value = localStore.remove(key, RemoveType.NORMAL);
      remoteRemoveObjectIfPossible((AbstractLocalCacheStoreValue) value);
    }
  }

  private void initiateLockRecall(LockID id) {
    if (id != null) {
      Set<LockID> lockID = Collections.singleton(id);
      l1LocalCacheManager.recallLocks(lockID);
    }
  }

  private void recallLocksInline(Set<LockID> lockIds) {
    l1LocalCacheManager.recallLocksInline(lockIds);
  }

  private void remoteRemoveObjectIfPossible(AbstractLocalCacheStoreValue removed) {
    if (removed != null) {
      ObjectID objectID = removed.getValueObjectId();
      if (isRemoteRemovePossible(objectID)) { return; }

      removeCallback.removedElement(removed);
    }
  }

  private boolean isRemoteRemovePossible(ObjectID objectId) {
    if (ObjectID.NULL_ID.equals(objectId)) { return false; }

    ReentrantReadWriteLock lock = lockProvider.getLock(objectId);
    lock.readLock().lock();
    try {
      if (transactionsInProgressObjectIDs.containsKey(objectId)) {
        removedObjectIDs.put(objectId, NULL_VALUE);
        return true;
      } else {
        return false;
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  public void transactionComplete(
                                  L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    l1LocalCacheManager.transactionComplete(l1ServerMapLocalStoreTransactionCompletionListener);
  }

  public void postTransactionCallback(
                                      Object key,
                                      AbstractLocalCacheStoreValue value,
                                      L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    final ObjectID objectId = value.getValueObjectId();
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
        remoteRemoveObjectIfPossible(value);
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
    return this.localStore.containsKeyOffHeap(key);
  }
}
