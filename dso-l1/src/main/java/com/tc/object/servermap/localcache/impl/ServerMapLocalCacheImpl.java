/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.LocalCacheAddCallBack;
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
import com.tc.object.servermap.localcache.PinnedEntryInvalidationListener;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalStoreTransactionCompletionListener.TransactionCompleteOperation;
import com.tc.object.servermap.localcache.impl.LocalStoreKeySet.LocalStoreKeySetFilter;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Eventual Mapping
 * <ul>
 * <li>key -> (value, valueoid)</li> (local ehcache)
 * <li>valueoid -> key</li> (local ehcache)
 * </ul>
 * <p/>
 * Strong Mapping (if value is not literal)
 * <ul>
 * <li>key -> (value, lockid)</li> (local ehcache)
 * <li>valueoid -> key</li> (local ehcache)
 * <li>lockid -> List&lt;valueoid></li> (In memory) lockIDMappings
 * </ul>
 * <p/>
 * Strong Mapping (if value is literal)
 * <ul>
 * <li>key -> (value, lockid)</li> (local ehcache)
 * <li>lockid -> List&lt;key></li> (In memory)
 * </ul>
 * <p/>
 * Bulk load
 * <ul>
 * <li>key -> (value, valueoid)</li>
 * <li>valueoid -> key</li>
 * </ul>
 */
public final class ServerMapLocalCacheImpl implements ServerMapLocalCache {
  private static final TCLogger                                LOGGER                           = TCLogging
                                                                                                    .getLogger(ServerMapLocalCacheImpl.class);
  private static final int                                     CONCURRENCY                      = 256;

  private static final LocalStoreKeySetFilter                  IGNORE_ID_FILTER                 = new IgnoreIdsFilter();
  private static final Object                                  NULL_VALUE                       = new Object();

  private final L1ServerMapLocalCacheManager                   l1LocalCacheManager;
  private volatile boolean                                     localCacheEnabled;
  private final L1ServerMapLocalCacheStore                     localStore;
  private final ClientObjectManager                            objectManager;
  private final Manager                                        manager;
  private final ServerMapLocalCacheRemoveCallback              removeCallback;

  private final ConcurrentMap<ObjectID, Object>                removedObjectIDs                 = new ConcurrentHashMap<ObjectID, Object>();
  private final Map<ObjectID, Object>                          oidsForWhichTxnAreInProgress     = new ConcurrentHashMap<ObjectID, Object>();

  private final ConcurrentHashMap                              pendingTransactionEntries;
  private final ConcurrentHashMap                              keyToListeners;

  private final ReentrantReadWriteLock[]                       locks;
  private final TCConcurrentMultiMap<LockID, ValueOIDKeyTuple> lockIDMappings;

  private final ConcurrentHashMap<ObjectID, Integer>           checkedOutObjects                = new ConcurrentHashMap<ObjectID, Integer>();

  private final Set<PinnedEntryInvalidationListener>           pinnedEntryInvalidationListeners = new CopyOnWriteArraySet<PinnedEntryInvalidationListener>();

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
    this.keyToListeners = new ConcurrentHashMap(CONCURRENCY, 0.75f, CONCURRENCY);
    this.lockIDMappings = new TCConcurrentMultiMap<LockID, ValueOIDKeyTuple>(CONCURRENCY, 0.75f, CONCURRENCY);
    this.locks = new ReentrantReadWriteLock[CONCURRENCY];
    for (int i = 0; i < CONCURRENCY; i++) {
      this.locks[i] = new ReentrantReadWriteLock();
    }
  }

  private ReentrantReadWriteLock getLock(Object key) {
    return locks[Math.abs(key.hashCode() % CONCURRENCY)];
  }

  @Override
  public L1ServerMapLocalCacheStore getInternalStore() {
    return localStore;
  }

  @Override
  public void setLocalCacheEnabled(boolean enable) {
    this.localCacheEnabled = enable;
  }

  @Override
  public void addToCache(final Object key, final AbstractLocalCacheStoreValue localCacheValue,
                         final MapOperationType mapOperation) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("XXX addToCache - key: " + key + ", vaue: " + localCacheValue + ", mapOperationType: "
                   + mapOperation);
    }

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
      keyToListeners.put(key, listener);
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
        localStore.put(valueObjectID, key);
      }
    }

    if (localCacheValue.isStrongConsistentValue()) {
      lockIDMappings.add(localCacheValue.asStrongValue().getLockId(), new ValueOIDKeyTuple(key, valueObjectID));
    }
  }

  private LockID removeMetaMapping(Object key, AbstractLocalCacheStoreValue localCacheValue,
                                   final boolean isRemoveFromInternalStore) {
    return removeMetaMapping(key, localCacheValue, isRemoveFromInternalStore, true);
  }

  private LockID removeMetaMapping(Object key, AbstractLocalCacheStoreValue localCacheValue,
                                   final boolean isRemoveFromInternalStore, boolean removedLockIdMapping) {
    if (localCacheValue == null || localCacheValue.isValueNull()) { return null; }

    if (!localCacheValue.isLiteral()) {
      // removing value oid mapping
      remove(localCacheValue.getValueObjectId(), isRemoveFromInternalStore);
    }

    if (removedLockIdMapping) {
      return removeLockIDMetaMapping(key, localCacheValue);
    } else {
      return null;
    }
  }

  private LockID removeLockIDMetaMapping(Object key, AbstractLocalCacheStoreValue localCacheValue) {
    if (localCacheValue == null) { return null; }

    if (localCacheValue.isStrongConsistentValue()) {
      // remove lockid mapping
      LockID lockID = localCacheValue.asStrongValue().getLockId();
      final boolean removeSuccess;
      removeSuccess = lockIDMappings.remove(lockID, new ValueOIDKeyTuple(key, localCacheValue.getValueObjectId()));
      return (removeSuccess && !lockIDMappings.containsKey(lockID)) ? lockID : null;
    }

    return null;
  }

  private L1ServerMapLocalStoreTransactionCompletionListener getTransactionCompleteListener(final Object key,
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

  @Override
  public void clear() {
    grabAllLocks();
    try {
      removeAllKeys(this.pendingTransactionEntries.keySet());
      removeAllKeys(this.localStore.getKeys());
    } finally {
      releaseAllLocks();
    }
  }

  @Override
  public void cleanLocalState() {
    this.pendingTransactionEntries.clear();
    this.localStore.cleanLocalState();
    this.oidsForWhichTxnAreInProgress.clear();
  }

  @Override
  public void clearInline() {
    Set<LockID> lockIDs = null;
    grabAllLocks();
    try {
      lockIDs = removeAllKeysWithInlineLockRecall(this.localStore.getKeys(), true);
      lockIDs.addAll(removeAllKeysWithInlineLockRecall(this.pendingTransactionEntries.keySet(), false));
    } finally {
      releaseAllLocks();
    }
    recallLocksInline(lockIDs);
  }

  private Set<LockID> removeAllKeysWithInlineLockRecall(Collection keySet, boolean removeFromInternalStore) {
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
    return lockIDs;
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

  private void releaseAllLocks() {
    for (ReentrantReadWriteLock lock : locks) {
      lock.writeLock().unlock();
    }
  }

  @Override
  public int size() {
    return this.localStore.size() + (this.pendingTransactionEntries.size() / 2);
  }

  @Override
  public void evictedInServer(Object key) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();

    try {
      removeFromInternalStore(key, null);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * When a remove from local cache is called, remove and flush
   */
  @Override
  public void removeFromLocalCache(Object key) {
    removeFromLocalCache(key, null);
  }

  @Override
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

  @Override
  public void entryEvicted(Object key, Object value) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("XXX entryEvicted " + key + " " + value);
    }
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
        AbstractLocalCacheStoreValue removed = (AbstractLocalCacheStoreValue) localStore.remove(key);
        if (removed != null) {
          initiateLockRecall(removeLockIDMetaMapping(key, removed));
        }
        remoteRemoveObjectIfPossible(removed);
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

  @Override
  public Set getKeys() {
    return Collections.unmodifiableSet(new LocalStoreKeySet(localStore.getKeys(), this.pendingTransactionEntries
        .keySet(), size(), IGNORE_ID_FILTER));
  }

  @Override
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
  @Override
  public AbstractLocalCacheStoreValue getLocalValue(final Object key) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.readLock().lock();
    try {
      return (AbstractLocalCacheStoreValue) getMappingUnlocked(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public AbstractLocalCacheStoreValue getLocalValueStrong(final Object key) {
    ReentrantReadWriteLock lock = getLock(key);

    AbstractLocalCacheStoreValue value = null;
    lock.readLock().lock();
    try {
      value = (AbstractLocalCacheStoreValue) getMappingUnlocked(key);
      if (value == null || isValidStrongValue(key, value)) { return value; }
    } finally {
      lock.readLock().unlock();
    }

    removeFromLocalCache(key, value);
    return null;
  }

  private boolean isValidStrongValue(Object key, AbstractLocalCacheStoreValue value) {
    if (!value.isStrongConsistentValue()) { return false; }
    if (value.isValueNull()) { return true; }
    LocalCacheStoreStrongValue strongValue = (LocalCacheStoreStrongValue) value;
    LockID lockId = strongValue.getLockId();
    Set<ValueOIDKeyTuple> set = this.lockIDMappings.get(lockId);
    if (set == null) { return false; }

    return set.contains(new ValueOIDKeyTuple(key, value.getValueObjectId()));
  }

  @Override
  public boolean removeEntriesForObjectId(ObjectID objectId) {
    Object key = getMappingUnlocked(objectId);
    if (key == null) { return false; }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("XXX removeEntriesForObjectId " + objectId + " key=" + key + " wasPinned: " + localStore.isPinned());
    }

    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();
    try {
      key = remove(objectId);
      if (key != null) {
        AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) remove(key);
        initiateLockRecall(removeLockIDMetaMapping(key, value));
        remoteRemoveObjectIfPossible(value);
        if (value != null && localStore.isPinned()) {
          notifyPinnedEntryInvalidated(key, value.isEventualConsistentValue());
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
    return true;
  }

  private void notifyPinnedEntryInvalidated(Object key, boolean eventual) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("XXX notify Invalidated key=" + key + "eventual: " + eventual);
    }
    for (PinnedEntryInvalidationListener listener : pinnedEntryInvalidationListeners) {
      listener.notifyKeyInvalidated(this, key, eventual);
    }

  }

  @Override
  public void removeEntriesForLockId(LockID lockId) {
    Set<ValueOIDKeyTuple> removedSet = this.lockIDMappings.removeAll(lockId);
    if (removedSet != null) {
      for (ValueOIDKeyTuple removedTuple : removedSet) {
        removeRemainingMappingsForLockID(removedTuple);
      }
    }
  }

  private void removeRemainingMappingsForLockID(ValueOIDKeyTuple removedTuple) {
    if (removedTuple == null) return;
    ReentrantReadWriteLock lock = getLock(removedTuple.key);
    lock.writeLock().lock();
    try {
      if (ObjectID.NULL_ID.equals(removedTuple.valueObjectID)) {
        // value was a literal so dont need to remote remove it
        AbstractLocalCacheStoreValue localValue = getLocalValue(removedTuple.key);
        remove(removedTuple.key);
        if (localValue != null && localStore.isPinned()) {
          notifyPinnedEntryInvalidated(removedTuple.key, localValue.isEventualConsistentValue());
        }
        // remoteRemoveObjectIfPossible((AbstractLocalCacheStoreValue) value);
      } else {
        removeEntriesForObjectId(removedTuple.valueObjectID);
      }
    } finally {
      lock.writeLock().unlock();
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("XXX remoteRemoveObjectIfPossible " + removed);
    }

    if (removed != null) {
      ObjectID objectID = removed.getValueObjectId();
      if (isRemoteRemovePossible(objectID)) {
        removedObjectIDs.remove(objectID);
        removeCallback.removedElement(removed);
      }
    }
  }

  private void remoteRemoveObjectIfPossible(TCObjectSelf removed) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("XXX remoteRemoveObjectIfPossible " + removed);
    }

    if (removed != null) {
      ObjectID objectID = removed.getObjectID();
      if (isRemoteRemovePossible(objectID)) {
        removedObjectIDs.remove(objectID);
        removeCallback.removedElement(removed);
      }
    }
  }

  /**
   * TODO: make sure this is always called within a lock
   */
  private boolean isRemoteRemovePossible(ObjectID objectId) {
    if (ObjectID.NULL_ID.equals(objectId)) { return false; }

    if (oidsForWhichTxnAreInProgress.containsKey(objectId) || checkedOutObjects.containsKey(objectId)) {
      removedObjectIDs.putIfAbsent(objectId, NULL_VALUE);
      return false;
    } else {
      return true;
    }
  }

  @Override
  public void transactionComplete(L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    l1LocalCacheManager.transactionComplete(l1ServerMapLocalStoreTransactionCompletionListener);
  }

  @Override
  public void postTransactionCallback(Object key, AbstractLocalCacheStoreValue value, boolean removeEntry,
                                      L1ServerMapLocalStoreTransactionCompletionListener listener) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();
    try {
      final ObjectID objectId = value.getValueObjectId();
      oidsForWhichTxnAreInProgress.remove(objectId);
      if (removedObjectIDs.containsKey(objectId)) {
        remoteRemoveObjectIfPossible(value);
      }

      if (keyToListeners.get(key) == listener) {
        keyToListeners.remove(key);
      } else {
        return;
      }
      if (removeEntry) {
        removeFromLocalCache(key, value);
      } else {
        Object valueFetched = pendingTransactionEntries.get(key);
        if (value.equals(valueFetched)) {
          // remove meta mappings, but don't remove lockIdMappings
          removeMetaMapping(key, value, false, false);
          addToCache(key, value, MapOperationType.GET);
          pendingTransactionEntries.remove(key, value);
        }
      }

      // use localStore directly instead of calling recalculateSize(key) as already under lock
      this.localStore.recalculateSize(key);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private static boolean isMetaInfoMapping(Object key) {
    return (key instanceof ObjectID || key instanceof LockID);
  }

  static class IgnoreIdsFilter implements LocalStoreKeySetFilter {

    @Override
    public boolean accept(Object value) {
      if (isMetaInfoMapping(value)) { return false; }
      return true;
    }

  }

  @Override
  public long onHeapSizeInBytes() {
    return this.localStore.onHeapSizeInBytes();
  }

  @Override
  public long offHeapSizeInBytes() {
    return this.localStore.offHeapSizeInBytes();
  }

  @Override
  public int onHeapSize() {
    int totalInMemoryCount = this.localStore.onHeapSize() + pendingTransactionEntries.size() / 2;
    return totalInMemoryCount < 0 ? Integer.MAX_VALUE : totalInMemoryCount;
  }

  @Override
  public int offHeapSize() {
    return this.localStore.offHeapSize();
  }

  @Override
  public boolean containsKeyOnHeap(Object key) {
    boolean containsKey = pendingTransactionEntries.containsKey(key);
    if (!containsKey) {
      containsKey = this.localStore.containsKeyOnHeap(key);
    }
    return containsKey;
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    return this.localStore.containsKeyOffHeap(key);
  }

  private AbstractLocalCacheStoreValue putKeyValueMapping(Object key, AbstractLocalCacheStoreValue value,
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
      Object serializeEntry = value.getValueObject();
      if (serializeEntry instanceof LocalCacheAddCallBack) {
        ((LocalCacheAddCallBack) serializeEntry).addedToLocalCache();
      }
      cleanupOldMetaMapping(key, value, old, true);
    }
    return old;
  }

  private void cleanupOldMetaMapping(Object key, AbstractLocalCacheStoreValue value, AbstractLocalCacheStoreValue old,
                                     boolean isRemoveFromInternalStore) {
    if (old == null || old.isValueNull() || old.isLiteral()) {
      // we don't put any meta mapping for REMOVE when value == null
      // when literal is present then lockid-> key will be present which we don want to remove
      return;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("XXX cleanupOldMetaMapping " + key + " " + old + " " + isRemoveFromInternalStore);
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
  TCConcurrentMultiMap<LockID, ValueOIDKeyTuple> getLockdIDMappings() {
    return this.lockIDMappings;
  }

  @Override
  public void recalculateSize(Object key) {
    if (key == null) { return; }
    ReentrantReadWriteLock rrwl = getLock(key);
    rrwl.writeLock().lock();
    try {
      this.localStore.recalculateSize(key);
    } finally {
      rrwl.writeLock().unlock();
    }
  }

  @Override
  public void checkInObject(Object key, Object valueParam) {
    if (valueParam == null || !(valueParam instanceof TCObjectSelf)) { return; }
    ObjectID oidParam = ((TCObjectSelf) valueParam).getObjectID();

    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();
    try {
      if (checkedOutObjects.get(oidParam) != null && checkedOutObjects.get(oidParam).intValue() == 1) {
        checkedOutObjects.remove(oidParam);
        if (removedObjectIDs.containsKey(oidParam)) {
          remoteRemoveObjectIfPossible((TCObjectSelf) valueParam);
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Object checkOutObject(Object key, Object valueParam) {
    if (valueParam == null || !(valueParam instanceof TCObjectSelf)) { return null; }
    ObjectID oidParam = ((TCObjectSelf) valueParam).getObjectID();

    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();
    try {
      AbstractLocalCacheStoreValue value = getLocalValue(key);
      if (value == null || !value.getValueObjectId().equals(oidParam)) { return null; }

      Integer checkedOutCount = checkedOutObjects.get(oidParam);
      checkedOutCount = checkedOutCount == null ? 1 : checkedOutCount + 1;

      checkedOutObjects.put(oidParam, checkedOutCount);
      return value.getValueObject();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public static class ValueOIDKeyTuple implements Serializable {
    private final Object   key;
    private final ObjectID valueObjectID;

    private ValueOIDKeyTuple(Object key, ObjectID valueObjectID) {
      this.key = key;
      this.valueObjectID = valueObjectID;
    }

    public Object getKey() {
      return key;
    }

    public ObjectID getValueObjectID() {
      return valueObjectID;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((valueObjectID == null) ? 0 : valueObjectID.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ValueOIDKeyTuple other = (ValueOIDKeyTuple) obj;
      if (key == null) {
        if (other.key != null) return false;
      } else if (!key.equals(other.key)) return false;
      if (valueObjectID == null) {
        if (other.valueObjectID != null) return false;
      } else if (!valueObjectID.equals(other.valueObjectID)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "ValueOIDKeyTuple [key=" + key + ", valueObjectID=" + valueObjectID + "]";
    }

  }

  @Override
  public boolean registerPinnedEntryInvalidationListener(PinnedEntryInvalidationListener listener) {
    return pinnedEntryInvalidationListeners.add(listener);
  }

  @Override
  public boolean unRegisterPinnedEntryInvalidationListener(PinnedEntryInvalidationListener listener) {
    return pinnedEntryInvalidationListeners.remove(listener);
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void transactionAbortedCallback(Object key, AbstractLocalCacheStoreValue value,
                                         L1ServerMapLocalStoreTransactionCompletionListener listener) {
    ReentrantReadWriteLock lock = getLock(key);
    lock.writeLock().lock();
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("XXX txnAbortedCallback " + key + " " + value);
      }
      final ObjectID objectId = value.getValueObjectId();
      oidsForWhichTxnAreInProgress.remove(objectId);
      if (keyToListeners.get(key) == listener) {
        keyToListeners.remove(key);
      } else {
        return;
      }
      Object valueFetched = pendingTransactionEntries.get(key);
      if (value.equals(valueFetched)) {
        // remove meta mappings, but don't remove lockIdMappings
        removeMetaMapping(key, value, false, false);
        pendingTransactionEntries.remove(key, value);
      }

    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void handleObjectIDsToValidate(Invalidations invalidations) {
    grabAllLocks();
    try {
      ObjectIDSet toRemoveSet = new ObjectIDSet();
      for (Object object : pendingTransactionEntries.keySet()) {
        if (object instanceof ObjectID) {
          toRemoveSet.add((ObjectID) object);
        }
      }
      invalidations.removeAll(toRemoveSet);
    } finally {
      releaseAllLocks();
    }
  }

  @Override
  public void addTxnInProgressKeys(Set<Object> addSet, Set<Object> removeSet) {
    grabAllLocks();
    try {
      for (Object object : pendingTransactionEntries.entrySet()) {
        Entry<Object, Object> entry = (Entry<Object, Object>) object;
        if (!(entry.getKey() instanceof ObjectID)) {
          AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) entry.getValue();
          if (value.getValueObject() != null) {
            addSet.add(entry.getKey());
          } else {
            removeSet.add(entry.getKey());
          }
        }
      }
    } finally {
      releaseAllLocks();
    }
  }

}
