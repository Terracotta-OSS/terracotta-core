/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreFullException;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalStoreTransactionCompletionListener.TransactionCompleteOperation;
import com.tc.object.servermap.localcache.impl.LocalStoreKeySet.LocalStoreKeySetFilter;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
  private static final int                           CONCURRENCY                                           = 256;

  private static final LocalStoreKeySetFilter        IGNORE_ID_FILTER                                      = new IgnoreIdsFilter();
  private static final Object                        NULL_VALUE                                            = new Object();

  private final L1ServerMapLocalCacheManager         l1LocalCacheManager;
  private volatile boolean                           localCacheEnabled;
  private final L1ServerMapLocalCacheStore           localStore;
  private final ClientObjectManager                  objectManager;
  private final Manager                              manager;
  private final ServerMapLocalCacheRemoveCallback    removeCallback;

  private final Map<ObjectID, Object>                removedObjectIDs                                      = new ConcurrentHashMap<ObjectID, Object>();
  private final Map<ObjectID, Object>                oidsForWhichTxnAreInProgress                          = new ConcurrentHashMap<ObjectID, Object>();

  private final ConcurrentHashMap                    pendingTransactionEntries;
  private final ReentrantReadWriteLock[]             locks;
  private final TCConcurrentMultiMap<LockID, Object> lockIDMappings;

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
    this.pendingTransactionEntries = new ConcurrentHashMap(CONCURRENCY, 0.75f, CONCURRENCY);
    this.lockIDMappings = new TCConcurrentMultiMap<LockID, Object>(CONCURRENCY, 0.75f, CONCURRENCY);
    this.locks = new ReentrantReadWriteLock[CONCURRENCY];
    for (int i = 0; i < CONCURRENCY; i++) {
      this.locks[i] = new ReentrantReadWriteLock();
    }
  }

  private ReentrantReadWriteLock getLock(Object key) {
    return locks[Math.abs(key.hashCode() % CONCURRENCY)];
  }

  public L1ServerMapLocalCacheStore getInternalStore() {
    return localStore;
  }

  public void setLocalCacheEnabled(boolean enable) {
    this.localCacheEnabled = enable;
  }

  public void addToCache(final Object key, final AbstractLocalCacheStoreValue localCacheValue,
                         final MapOperationType mapOperation) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();

    try {
      if (localCacheValue.isStrongConsistentValue()) {
        l1LocalCacheManager.rememberMapIdForValueLockId(localCacheValue.asStrongValue().getLockId(), this);
      }

      putMetaMapping(localCacheValue, key, mapOperation);
      putKeyValueMapping(key, localCacheValue, mapOperation);

      if (mapOperation.isMutateOperation() && !localCacheValue.isValueNull() && !localCacheValue.isLiteral()) {
        oidsForWhichTxnAreInProgress.put(localCacheValue.getValueObjectId(), NULL_VALUE);
      }

      registerTransactionCompleteListener(key, localCacheValue, mapOperation);
    } catch (LocalCacheStoreFullException e) {
      if (mapOperation.isMutateOperation()) { throw new AssertionError(
                                                                       "Tried to put directly into local cache for mutate operation."); }
      LOGGER.warn("Insufficient local cache memory to store the value for key " + key);
      Object old = remove(key, true);
      if (old != null) {
        // This is here to handle the case where we have an existing entry in the local cache to replace. For example,
        // two consecutive puts.
        handleKeyValueMappingRemoved(key, (AbstractLocalCacheStoreValue) old, true);
      }
      initiateLockRecall(removeMetaMapping(key, localCacheValue, true));
      remoteRemoveObjectIfPossible(localCacheValue);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void registerTransactionCompleteListener(final Object key,
                                                   final AbstractLocalCacheStoreValue localCacheValue,
                                                   final MapOperationType mapOperation) throws AssertionError {
    if (mapOperation.isMutateOperation()) {
      L1ServerMapLocalStoreTransactionCompletionListener listener = getTransactionCompleteListener(key,
                                                                                                   localCacheValue,
                                                                                                   mapOperation);
      if (listener == null) { throw new AssertionError("Transaction Complete Listener cannot be null for mutate ops"); }
      ClientTransaction txn = this.objectManager.getTransactionManager().getCurrentTransaction();
      if (txn == null) { throw new UnlockedSharedObjectException(
                                                                 "Attempt to access a shared object outside the scope of a shared lock.",
                                                                 Thread.currentThread().getName(), manager
                                                                     .getClientID()); }
      txn.addTransactionCompleteListener(listener);
    }
  }

  private void putMetaMapping(final AbstractLocalCacheStoreValue localCacheValue, final Object key,
                              MapOperationType mapOperation) {
    if (localCacheValue.getValueObject() == null) { return; }

    ObjectID valueObjectID = localCacheValue.getValueObjectId();
    if (!valueObjectID.equals(ObjectID.NULL_ID)) {
      // this is for non literal keys
      // literal keys need to put lock id mapping
      if (mapOperation.isMutateOperation()) {
        // put a pinned entry for mutate ops, unpinned on txn complete
        this.pendingTransactionEntries.put(valueObjectID, key);
      } else {
        this.localStore.put(valueObjectID, key);
      }
    }

    if (localCacheValue.isStrongConsistentValue()) {
      Object valueToPut = valueObjectID.equals(ObjectID.NULL_ID) ? key : valueObjectID;
      lockIDMappings.add(localCacheValue.asStrongValue().getLockId(), valueToPut);
    }
  }

  private LockID removeMetaMapping(Object key, AbstractLocalCacheStoreValue localCacheValue,
                                   final boolean isRemoveFromInternalStore) {
    if (localCacheValue == null || localCacheValue.isValueNull()) { return null; }

    if (!localCacheValue.isLiteral()) {
      // removing value oid mapping
      remove(localCacheValue.getValueObjectId(), isRemoveFromInternalStore);
    }

    return removeLockIDMetaMapping(key, localCacheValue);
  }

  private LockID removeLockIDMetaMapping(Object key, AbstractLocalCacheStoreValue localCacheValue) {
    if (localCacheValue == null) { return null; }

    if (localCacheValue.isStrongConsistentValue()) {
      // remove lockid mapping
      LockID lockID = localCacheValue.asStrongValue().getLockId();
      if (localCacheValue.isLiteral()) {
        lockIDMappings.remove(lockID, key);
      } else {
        lockIDMappings.remove(lockID, localCacheValue.getValueObjectId());
      }
      return lockIDMappings.containsKey(lockID) ? null : lockID;
    }

    return null;
  }

  private L1ServerMapLocalStoreTransactionCompletionListener getTransactionCompleteListener(
                                                                                            final Object key,
                                                                                            AbstractLocalCacheStoreValue value,
                                                                                            MapOperationType mapOperation) {
    if (!mapOperation.isMutateOperation()) {
      // no listener required for non mutate ops
      return null;
    }
    final TransactionCompleteOperation onTransactionComplete;
    if (localCacheEnabled) {
      // when local cache is enabled, remove the cached value if the operation is a REMOVE, otherwise just unpin
      onTransactionComplete = mapOperation.isRemoveOperation() || value.isLiteral() ? TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY
          : TransactionCompleteOperation.UNPIN_ENTRY;
    } else {
      // when local cache is disabled, always remove the cached value on txn complete
      onTransactionComplete = TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY;
    }
    return new L1ServerMapLocalStoreTransactionCompletionListener(this, key, value, onTransactionComplete);
  }

  public void clear() {
    grabAllLocks();
    try {
      removeAllKeys(this.pendingTransactionEntries.keySet());
      removeAllKeys(this.localStore.getKeys());
    } finally {
      realeseAllLocks();
    }
  }

  /**
   * unpin all pinned keys
   */
  public void unpinAll() {
    this.localStore.unpinAll();
  }

  /**
   * check the key is pinned or not
   */
  public boolean isPinned(Object key) {
    return this.localStore.isPinned(key);
  }

  /**
   * pin or unpin the key
   */
  public void setPinned(Object key, boolean pinned) {
    this.localStore.setPinned(key, pinned);
  }

  public void clearInline() {
    grabAllLocks();
    try {
      removeAllKeysWithInlineLockRecall(this.localStore.getKeys(), true);
      removeAllKeysWithInlineLockRecall(this.pendingTransactionEntries.keySet(), false);
    } finally {
      realeseAllLocks();
    }
  }

  private void removeAllKeysWithInlineLockRecall(Collection keySet, boolean removeFromInternalStore) {
    Set<LockID> lockIDs = new HashSet<LockID>();
    for (Object key : keySet) {
      if (!isMetaInfoMapping(key)) {
        Object value = remove(key, removeFromInternalStore);
        if (value instanceof AbstractLocalCacheStoreValue) {
          AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) value;
          LockID lockID = handleKeyValueMappingRemoved(key, localValue, removeFromInternalStore, false);
          if (lockID != null) {
            lockIDs.add(lockID);
          }
        }
      }
    }
    recallLocksInline(lockIDs);
  }

  private void removeAllKeys(Collection keySet) {
    for (Object key : keySet) {
      if (!isMetaInfoMapping(key)) {
        removeFromLocalCache(key);
      }
    }
  }

  private void grabAllLocks() {
    for (ReentrantReadWriteLock lock : locks) {
      lock.writeLock().lock();
    }
  }

  private void realeseAllLocks() {
    for (ReentrantReadWriteLock lock : locks) {
      lock.writeLock().unlock();
    }
  }

  public int size() {
    return this.localStore.size() + (this.pendingTransactionEntries.size() / 2);
  }

  /**
   * When a remove from local cache is called, remove and flush
   */
  public void removeFromLocalCache(Object key) {
    removeFromLocalCache(key, null);
  }

  public void removeFromLocalCache(Object key, AbstractLocalCacheStoreValue value) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();

    try {
      if (!removeFromPendingTransactionEntries(key, value)) {
        removeFromInternalStore(key, value);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void removeFromInternalStore(Object key, AbstractLocalCacheStoreValue value) {
    final Object oldValue;
    if (value == null) {
      oldValue = localStore.remove(key);
    } else {
      oldValue = localStore.remove(key, value);
    }
    if (oldValue instanceof AbstractLocalCacheStoreValue) {
      AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) oldValue;
      handleKeyValueMappingRemoved(key, localValue, true);
    }
  }

  private boolean removeFromPendingTransactionEntries(Object key, AbstractLocalCacheStoreValue value) {
    boolean removed;
    if (value == null) {
      value = (AbstractLocalCacheStoreValue) pendingTransactionEntries.remove(key);
      removed = value != null;
    } else {
      removed = pendingTransactionEntries.remove(key, value);
    }

    if (removed) {
      handleKeyValueMappingRemoved(key, value, false);
    }
    return removed;
  }

  public void entryEvicted(Object key, Object value) {
    if (key instanceof ObjectID) {
      objectIDMappingEvicted((ObjectID) key, value);
    } else {
      keyValueMappingEvicted(key, (AbstractLocalCacheStoreValue) value);
    }
  }

  private void keyValueMappingEvicted(Object key, AbstractLocalCacheStoreValue value) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();
    try {
      handleKeyValueMappingRemoved(key, value, true);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void objectIDMappingEvicted(ObjectID oid, Object key) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();
    try {
      AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) localStore.get(key);
      if (value != null && value.getValueObjectId().equals(oid)) {
        if (localStore.isPinned(key)) {
          localStore.put(oid, key);
        } else {
          AbstractLocalCacheStoreValue removed = (AbstractLocalCacheStoreValue) localStore.remove(key);
          if (removed != null) {
            initiateLockRecall(removeLockIDMetaMapping(key, removed));
          }
          remoteRemoveObjectIfPossible(removed);
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void handleKeyValueMappingRemoved(Object key, AbstractLocalCacheStoreValue value,
                                            boolean removeFromInternalStore) {
    handleKeyValueMappingRemoved(key, value, removeFromInternalStore, true);
  }

  private LockID handleKeyValueMappingRemoved(Object key, AbstractLocalCacheStoreValue value,
                                              boolean removeFromInternalStore, boolean initiateLockRecall) {
    LockID lockID = removeMetaMapping(key, value, removeFromInternalStore);
    // this is for handling leaking lock id .. get, remove
    if (lockID == null && value.isStrongConsistentValue() && value.isValueNull()
        && !lockIDMappings.containsKey(value.asStrongValue().getLockId())) {
      lockID = value.asStrongValue().getLockId();
    }

    remoteRemoveObjectIfPossible(value);
    if (initiateLockRecall) {
      initiateLockRecall(lockID);
    }

    return lockID;
  }

  public Set getKeys() {
    return Collections.unmodifiableSet(new LocalStoreKeySet(localStore.getKeys(), this.pendingTransactionEntries
        .keySet(), size(), IGNORE_ID_FILTER));
  }

  public Object getMappingUnlocked(final Object keyOrId) {
    Object rv = pendingTransactionEntries.get(keyOrId);
    if (rv != null) { return rv; }

    rv = this.localStore.get(keyOrId);
    if (rv instanceof AbstractLocalCacheStoreValue) {
      AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) rv;
      if (localValue.getValueObject() instanceof TCObjectSelf) {
        this.l1LocalCacheManager.initializeTCObjectSelfIfRequired((TCObjectSelf) localValue.getValueObject());
      }
    }
    return rv;
  }

  /**
   * Returned value may be coherent or incoherent or null
   */
  public AbstractLocalCacheStoreValue getLocalValue(final Object key) {
    final AbstractLocalCacheStoreValue value;
    ReentrantReadWriteLock lock = getLock(key);
    lock.readLock().lock();
    try {
      value = (AbstractLocalCacheStoreValue) getMappingUnlocked(key);
    } finally {
      lock.readLock().unlock();
    }
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
  public AbstractLocalCacheStoreValue getLocalValueCoherent(final Object key) {
    final AbstractLocalCacheStoreValue value;
    ReentrantReadWriteLock lock = getLock(key);
    lock.readLock().lock();
    try {
      value = (AbstractLocalCacheStoreValue) getMappingUnlocked(key);
    } finally {
      lock.readLock().unlock();
    }
    if (value != null && value.isIncoherentValue()) {
      // don't return incoherent items from here
      removeFromLocalCache(key, value);
      return null;
    }
    return value;
  }

  public AbstractLocalCacheStoreValue getLocalValueStrong(final Object key) {
    final AbstractLocalCacheStoreValue value = getLocalValueCoherent(key);
    if (value != null && !isValidStrongValue(key, value)) {
      removeFromLocalCache(key, value);
      return null;
    }
    return value;
  }

  private boolean isValidStrongValue(Object key, AbstractLocalCacheStoreValue value) {
    if (!value.isStrongConsistentValue()) { return false; }
    if (value.isValueNull()) { return true; }
    LocalCacheStoreStrongValue strongValue = (LocalCacheStoreStrongValue) value;
    LockID lockId = strongValue.getLockId();
    Set set = this.lockIDMappings.get(lockId);
    if (set == null) { return false; }

    if (value.isLiteral()) {
      return set.contains(key);
    } else {
      return set.contains(value.getValueObjectId());
    }
  }

  private boolean isIncoherentTooLong(AbstractLocalCacheStoreValue value) {
    long lastCoherentTime = value.asIncoherentValue().getLastCoherentTime();
    return TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - lastCoherentTime)) >= SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS;
  }

  public boolean removeEntriesForObjectId(ObjectID objectId) {
    Object key = getMappingUnlocked(objectId);
    if (key == null) { return false; }

    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();

    try {
      key = remove(objectId);
      if (key != null) {
        AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) remove(key);
        initiateLockRecall(removeLockIDMetaMapping(key, value));
        remoteRemoveObjectIfPossible(value);
      }
    } finally {
      lock.writeLock().unlock();
    }
    return true;
  }

  public void removeEntriesForLockId(LockID lockId) {
    Set removedSet = this.lockIDMappings.removeAll(lockId);
    if (removedSet != null) {
      for (Object removed : removedSet) {
        removeRemainingMappingsForLockID(removed);
      }
    }
  }

  private void removeRemainingMappingsForLockID(Object keyOrValueId) {
    if (keyOrValueId instanceof ObjectID) {
      removeEntriesForObjectId((ObjectID) keyOrValueId);
    } else if (keyOrValueId != null) {
      Object key = keyOrValueId;
      ReentrantReadWriteLock lock = getLock(key);
      lock.writeLock().lock();

      try {
        // value was a literal so dont need to remote remove it
        remove(key);
        // remoteRemoveObjectIfPossible((AbstractLocalCacheStoreValue) value);
      } finally {
        lock.writeLock().unlock();
      }
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
      if (isRemoteRemovePossible(objectID)) {
        removeCallback.removedElement(removed);
      }
    }
  }

  /**
   * TODO: make sure this is always called within a lock
   */
  private boolean isRemoteRemovePossible(ObjectID objectId) {
    if (ObjectID.NULL_ID.equals(objectId)) { return false; }

    if (oidsForWhichTxnAreInProgress.containsKey(objectId)) {
      removedObjectIDs.put(objectId, NULL_VALUE);
      return false;
    } else {
      return true;
    }
  }

  public void transactionComplete(
                                  L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    l1LocalCacheManager.transactionComplete(l1ServerMapLocalStoreTransactionCompletionListener);
  }

  public void postTransactionCallback(Object key, AbstractLocalCacheStoreValue value, boolean removeEntry) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();

    try {
      final ObjectID objectId = value.getValueObjectId();
      oidsForWhichTxnAreInProgress.remove(objectId);
      if (removedObjectIDs.remove(objectId) != null) {
        remoteRemoveObjectIfPossible(value);
      }

      if (removeEntry) {
        removeFromLocalCache(key, value);
      } else {
        boolean isRemoved = pendingTransactionEntries.remove(key, value);
        if (isRemoved) {
          removeMetaMapping(key, value, false);
          addToCache(key, value, MapOperationType.GET);
        }
      }
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
    int totalInMemoryCount = this.localStore.onHeapSize() + pendingTransactionEntries.size() / 2;
    if (this.localStore.getMaxElementsInMemory() > 0) {
      return Math.min(totalInMemoryCount, this.localStore.getMaxElementsInMemory());
    } else {
      return totalInMemoryCount;
    }
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

  public AbstractLocalCacheStoreValue putKeyValueMapping(Object key, AbstractLocalCacheStoreValue value,
                                                         MapOperationType mapOperation) {
    AbstractLocalCacheStoreValue old;
    if (mapOperation.isMutateOperation()) {
      // put a pinned entry for mutate ops, unpinned on txn complete
      old = (AbstractLocalCacheStoreValue) this.pendingTransactionEntries.put(key, value);
      if (old != null) {
        cleanupOldMetaMapping(key, value, old, false);
      } else {
        old = (AbstractLocalCacheStoreValue) this.localStore.remove(key);
        cleanupOldMetaMapping(key, value, old, true);
      }
    } else {
      old = (AbstractLocalCacheStoreValue) this.localStore.put(key, value);
      cleanupOldMetaMapping(key, value, old, true);
    }
    return old;
  }

  private void cleanupOldMetaMapping(Object key, AbstractLocalCacheStoreValue value, AbstractLocalCacheStoreValue old,
                                     boolean isRemoveFromInternalStore) {
    if (old == null || old.isValueNull() || old.isLiteral()) {
      // we dont put any meta mapping for REMOVE when value == null
      // when literal is present then lockid-> key will be present which we don want to remove
      return;
    }

    removeMetaMapping(key, old, isRemoveFromInternalStore);
    remoteRemoveObjectIfPossible(old);
  }

  private Object remove(Object key) {
    Object rv = null;
    if ((rv = this.pendingTransactionEntries.remove(key)) == null) {
      rv = this.localStore.remove(key);
    }
    return rv;
  }

  private Object remove(Object key, boolean fromInternalStore) {
    if (fromInternalStore) {
      return this.localStore.remove(key);
    } else {
      return this.pendingTransactionEntries.remove(key);
    }
  }

  // used for tests only
  TCConcurrentMultiMap<LockID, Object> getLockdIDMappings() {
    return this.lockIDMappings;
  }
}
