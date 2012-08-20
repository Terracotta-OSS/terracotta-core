/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheConfigFields;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheMetaDataCallback;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.meta.MetaData;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.terracotta.toolkit.TerracottaProperties;
import com.terracotta.toolkit.concurrent.locks.LockingUtils;
import com.terracotta.toolkit.concurrent.locks.LongLockStrategy;
import com.terracotta.toolkit.concurrent.locks.UnnamedToolkitLock;
import com.terracotta.toolkit.concurrent.locks.UnnamedToolkitReadWriteLock;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.meta.Extractor;
import com.terracotta.toolkit.object.AbstractTCToolkitObject;
import com.terracotta.toolkit.object.serialization.CustomLifespanSerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializedMapValueParameters;
import com.terracotta.toolkit.search.SearchConstants;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ServerMap<K, V> extends AbstractTCToolkitObject implements InternalToolkitMap<K, V>, NotClearable {
  private static final TCLogger                                 LOGGER                   = TCLogging
                                                                                             .getLogger(ServerMap.class);
  private static final Object[]                                 NO_ARGS                  = new Object[0];
  private static final ToolkitLock                              EXPIRE_CONCURRENT_LOCK   = new UnnamedToolkitLock(
                                                                                                                  "servermap-static-expire-concurrent-lock",
                                                                                                                  ToolkitLockTypeInternal.CONCURRENT);
  private static final ToolkitLock                              EVENTUAL_CONCURRENT_LOCK = new UnnamedToolkitLock(
                                                                                                                  "servermap-static-eventual-concurrent-lock",
                                                                                                                  ToolkitLockTypeInternal.CONCURRENT);

  private static final boolean                                  DEBUG_EXPIRATION         = new TerracottaProperties()
                                                                                             .getBoolean("toolkit.map.expiration.debug",
                                                                                                         false);

  // clustered fields
  private final ToolkitLockTypeInternal                         lockType;
  private volatile boolean                                      localCacheEnabled;
  private volatile boolean                                      compressionEnabled;
  private volatile boolean                                      copyOnReadEnabled;
  private volatile int                                          maxTTISeconds;
  private volatile int                                          maxTTLSeconds;
  private volatile int                                          maxCountInCluster;

  // unclustered local fields
  protected volatile TCObjectServerMap<Long>                    tcObjectServerMap;
  protected volatile L1ServerMapLocalCacheStore                 l1ServerMapLocalCacheStore;
  protected volatile LongLockStrategy                           lockStrategy;
  private volatile String                                       instanceDsoLockName      = null;
  private volatile CopyOnWriteArraySet<ToolkitCacheListener<K>> listeners;
  private volatile Collection<V>                                values                   = null;
  private volatile TimeSource                                   timeSource;

  private final String                                          name;
  private final Consistency                                     consistency;
  private volatile ToolkitCacheMetaDataCallback                 metaDataCallback;

  public ServerMap(Configuration config, String name) {
    this.name = name;
    String consistencyStr = (String) InternalCacheConfigurationType.CONSISTENCY.getExistingValueOrException(config);
    this.consistency = Consistency.valueOf(consistencyStr);
    ToolkitLockTypeInternal tmpLockType = null;
    switch (this.consistency) {
      case EVENTUAL:
        tmpLockType = ToolkitLockTypeInternal.CONCURRENT;
        break;
      case SYNCHRONOUS_STRONG:
        tmpLockType = ToolkitLockTypeInternal.SYNCHRONOUS_WRITE;
        break;
      case STRONG:
        tmpLockType = ToolkitLockTypeInternal.WRITE;
        break;
    }
    this.lockType = tmpLockType;

    this.localCacheEnabled = (Boolean) InternalCacheConfigurationType.LOCAL_CACHE_ENABLED
        .getExistingValueOrException(config);
    this.maxCountInCluster = (Integer) InternalCacheConfigurationType.MAX_TOTAL_COUNT
        .getExistingValueOrException(config);
    this.maxTTISeconds = (Integer) InternalCacheConfigurationType.MAX_TTI_SECONDS.getExistingValueOrException(config);
    this.maxTTLSeconds = (Integer) InternalCacheConfigurationType.MAX_TTL_SECONDS.getExistingValueOrException(config);

    this.listeners = new CopyOnWriteArraySet<ToolkitCacheListener<K>>();
    this.timeSource = new SystemTimeSource();
    this.compressionEnabled = (Boolean) InternalCacheConfigurationType.COMPRESSION_ENABLED
        .getExistingValueOrException(config);
    this.copyOnReadEnabled = (Boolean) InternalCacheConfigurationType.COPY_ON_READ_ENABLED
        .getExistingValueOrException(config);
  }

  @Override
  public void initializeLocalCache(L1ServerMapLocalCacheStore<K, V> localCacheStore) {
    if (localCacheStore == null) { throw new AssertionError("Local Cache Store cannot be null"); }
    this.l1ServerMapLocalCacheStore = localCacheStore;
    this.tcObjectServerMap.initialize(maxTTISeconds, maxTTLSeconds, maxCountInCluster, isEventual(), localCacheEnabled);
    this.tcObjectServerMap.setupLocalStore(l1ServerMapLocalCacheStore);
  }

  @Override
  public void __tc_managed(TCObject t) {
    super.__tc_managed(t);
    if (!(t instanceof TCObjectServerMap)) { throw new AssertionError("Wrong tc object created for ServerMap - " + t); }
    this.tcObjectServerMap = (TCObjectServerMap) t;
    this.lockStrategy = new LongLockStrategy<K>(getInstanceDsoLockName());
  }

  // Internal methods used by applicator
  @Override
  public ToolkitLockTypeInternal getLockType() {
    return lockType;
  }

  @Override
  public boolean isEventual() {
    return this.consistency == Consistency.EVENTUAL;
  }

  @Override
  public String getName() {
    return name;
  }

  private Long generateLockIdForKey(final Object key) {
    return lockStrategy.generateLockIdForKey(key);
  }

  private String getInstanceDsoLockName() {
    if (this.instanceDsoLockName != null) { return this.instanceDsoLockName; }

    // The trailing colon (':') is relevant to avoid unintended lock collision when appending object ID to the key
    // Without the delimiter you'd get the same effective lock for oid 11, lockId 10 and oid 1 and lockId 110
    this.instanceDsoLockName = "@ServerMap:" + ((Manageable) this).__tc_managed().getObjectID().toLong() + ":";
    return this.instanceDsoLockName;
  }

  private void commitLock(final Long lockID, final ToolkitLockTypeInternal type) {
    this.lockStrategy.commitLock(lockID, type);
  }

  private void beginLock(final Long lockID, final ToolkitLockTypeInternal type) {
    this.lockStrategy.lock(lockID, type);
  }

  private V doGet(Object key, GetType getType, boolean quiet) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return null;
    }

    SerializedMapValue serializedMapValue = getSerializedMapValue(key, getType);
    return getNonExpiredValue(key, serializedMapValue, getType, quiet);
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    final GetType getType = isEventual() ? GetType.UNLOCKED : GetType.LOCKED;
    SerializedMapValue serializedMapValue = getSerializedMapValue(key, getType);
    if (serializedMapValue != null && serializedMapValue.getObjectID().equals(valueOid)) {
      // make sure key is mapped to same valueOid
      return getNonExpiredValue(key, serializedMapValue, getType, false);
    } else {
      return null;
    }
  }

  private SerializedMapValue getSerializedMapValue(Object key, GetType getType) {
    assertKeyLiteral(key);
    SerializedMapValue serializedMapValue = null;
    switch (getType) {
      case LOCKED:
        final Long lockID = generateLockIdForKey(key);
        beginLock(lockID, ToolkitLockTypeInternal.READ);
        try {
          Object obj = doLogicalGetValueLocked(key, lockID);
          serializedMapValue = asSerializedMapValue(obj);
        } finally {
          commitLock(lockID, ToolkitLockTypeInternal.READ);
        }
        break;
      case UNLOCKED:
        // use unlocked api to fetch values
        serializedMapValue = asSerializedMapValue(doLogicalGetValueUnlocked(key));
        break;
      case UNSAFE:
        // use only local cache to look up value
        try {
          serializedMapValue = asSerializedMapValue(this.tcObjectServerMap.getValueFromLocalCache(key));
        } catch (TCNotRunningException e) {
          // ignore TCNotRunningException for unsafe gets
          return null;
        }
        break;
    }
    return serializedMapValue;
  }

  private SerializedMapValue asSerializedMapValue(Object obj) {
    if (obj != null) {
      if (obj instanceof SerializedMapValue) {
        return (SerializedMapValue) obj;
      } else {
        //
        throw new AssertionError("Value is not instanceof SerializedMapValue: " + obj);
      }
    }
    return null;
  }

  private V getNonExpiredValue(Object key, SerializedMapValue serializedMapValue, GetType getType, boolean quiet) {
    if (serializedMapValue == null) { return null; }

    if (getType == GetType.UNSAFE) {
      // don't touch tc layer when doing unsafe reads
      return deserialize(key, serializedMapValue);
    }
    serializedMapValue = expireEntryIfNecessary(key, serializedMapValue, getType, quiet);
    return deserialize(key, serializedMapValue);
  }

  @Override
  public V checkAndGetNonExpiredValue(K key, Object value, GetType getType, boolean quiet) {
    SerializedMapValue serializedMapValue = asSerializedMapValue(value);
    return deserialize(key, expireEntryIfNecessary(key, serializedMapValue, getType, quiet));
  }

  private SerializedMapValue expireEntryIfNecessary(Object key, SerializedMapValue serializedMapValue, GetType getType,
                                                    boolean quiet) {
    if (serializedMapValue == null) return null;
    if (isExpirationEnabled() || serializedMapValue instanceof CustomLifespanSerializedMapValue) {
      // check for expiration
      int now = timeSource.nowInSeconds();
      final boolean expired;
      if (serializedMapValue.isExpired(now, maxTTISeconds, maxTTLSeconds)) {
        boolean readLocked = createLockForKey(key).readLock().isHeldByCurrentThread();
        if (!readLocked) {
          expire(key, serializedMapValue, getType);
        }
        expired = true;
        serializedMapValue = null;
      } else {
        if (!quiet) {
          markUsed(key, serializedMapValue, now);
        }
        expired = false;
      }
      if (DEBUG_EXPIRATION) {
        LOGGER.info("Expiration debug - Key: " + key + ", now: " + now + ", maxTTISeconds: " + maxTTISeconds
                    + ", maxTTLSeconds: " + maxTTLSeconds + ", expired: " + expired + ", serializedMapValue: "
                    + serializedMapValue);
      }
    }
    return serializedMapValue;
  }

  private V deserialize(Object key, SerializedMapValue serializedMapValue) {
    if (serializedMapValue == null) { return null; }
    try {
      V deserialized = null;

      if (copyOnReadEnabled) {
        deserialized = (V) serializedMapValue.getDeserializedValueCopy(strategy, compressionEnabled);
      } else {
        deserialized = (V) serializedMapValue.getDeserializedValue(strategy, compressionEnabled,
                                                                   l1ServerMapLocalCacheStore, key);
      }
      return deserialized;
    } catch (Exception e) {
      // TODO: handle differently?
      throw new RuntimeException(e);
    }
  }

  private void markUsed(Object key, SerializedMapValue serializedMapValue,
                        int usedAtTime) {
    if (shouldUpdateIdleTimer(usedAtTime, maxTTISeconds, serializedMapValue.internalGetLastAccessedTime())) {
      serializedMapValue.updateLastAccessedTime(key, tcObjectServerMap, usedAtTime);
    }
  }

  /**
   * Only want to update the expire time if we are at least half way through the idle period.
   * 
   * @param usedAtTime The time when the item is being used (==now) in seconds since the epoch
   * @return True if should update, false to skip it
   */
  private boolean shouldUpdateIdleTimer(final int usedAtTime, final int configTTI, int lastAccessedTime) {
    if (configTTI == ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS) { return false; }
    final int timeSinceUsed = usedAtTime - lastAccessedTime;

    // only bother to update TTI if we're at least half way through the TTI period
    final int halfTTI = configTTI / 2;
    if (timeSinceUsed >= halfTTI) {
      return true;
    } else {
      return false;
    }
  }

  private void expire(Object key, SerializedMapValue serializedMapValue, GetType getType) {
    final boolean notify;

    V deserializedValue = deserialize(key, serializedMapValue);
    MetaData metaData = getEvictRemoveMetaData();

    if (getType == GetType.LOCKED) {
      boolean removed = removeWithMetaData(key, deserializedValue, metaData);

      if (removed) {
        notify = true;
      } else {
        notify = false;
      }
    } else {
      EXPIRE_CONCURRENT_LOCK.lock();
      try {
        boolean mutated = this.tcObjectServerMap.doLogicalRemoveUnlocked(this, key, serializedMapValue);
        if (mutated && metaData != null) {
          metaData.set(SearchConstants.Meta.COMMAND, SearchConstants.Commands.REMOVE_IF_VALUE_EQUAL);
          metaData.add("", 1);
          metaData.add("", key);
          metaData.add("", serializedMapValue.getObjectID());
          addMetaData(metaData);
        }
      } finally {
        EXPIRE_CONCURRENT_LOCK.unlock();
        notify = true;
      }
    }
    if (notify) {
      notifyElementExpired((K) key);
    }
  }

  private MetaData getEvictRemoveMetaData() {
    if (metaDataCallback != null) { return metaDataCallback.getEvictRemoveMetaData(); }
    return null;
  }

  private boolean isExpirationEnabled() {
    return maxTTISeconds != ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS
           || maxTTLSeconds != ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS;
  }

  /**
   * Throws NPE if metaData is null
   */
  private void addMetaData(MetaData metaData) {
    MetaDataDescriptor mdd = Extractor.extractInternalDescriptorFrom(metaData);
    tcObjectServerMap.addMetaData(mdd);
  }

  private Object doLogicalGetValueLocked(Object key, final Long lockID) {
    return this.tcObjectServerMap.getValue(this, lockID, key);
  }

  private Object doLogicalGetValueUnlocked(Object key) {
    return this.tcObjectServerMap.getValueUnlocked(this, key);
  }

  private void doLogicalPutLocked(final Long lockID, K key, final V value, int createTimeInSecs,
                                  int customMaxTTISeconds, int customMaxTTLSeconds, final MetaData metaData) {
    doLogicalPut(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData, MutateType.LOCKED,
                 lockID);
  }

  private void doLogicalPutUnlocked(K key, final V value, int createTimeInSecs, int customMaxTTISeconds,
                                    int customMaxTTLSeconds, final MetaData metaData) {
    doLogicalPut(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData, MutateType.UNLOCKED,
                 null);
  }

  private void doLogicalPut(K key, final V value, int createTimeInSecs, int customMaxTTISeconds,
                            int customMaxTTLSeconds, final MetaData metaData, final MutateType type, final Long lockID) {
    K portableKey = (K) assertKeyLiteral(key);
    SerializedMapValue serializedMapValue = createSerializedMapValue(value, createTimeInSecs, customMaxTTISeconds,
                                                                     customMaxTTLSeconds);

    switch (type) {
      case LOCKED:
        assertNotNull(lockID);
        this.tcObjectServerMap.doLogicalPut(this, lockID, portableKey, serializedMapValue);
        break;
      case UNLOCKED:
        assertNull(lockID);
        this.tcObjectServerMap.doLogicalPutUnlocked(this, portableKey, serializedMapValue);
        break;
    }

    if (metaData != null) {
      metaData.set(SearchConstants.Meta.COMMAND, SearchConstants.Commands.PUT);
      metaData.add(SearchConstants.Meta.VALUE, serializedMapValue.getObjectID());
      addMetaData(metaData);
    }
  }

  private void doLogicalRemoveLocked(Object key, final MetaData metaData, final Long lockID) {
    doLogicalRemove(key, metaData, MutateType.LOCKED, lockID);
  }

  private void doLogicalRemoveUnlocked(Object key, final MetaData metaData) {
    doLogicalRemove(key, metaData, MutateType.UNLOCKED, null);
  }

  private void doLogicalRemove(final Object key, final MetaData metaData, MutateType type, Long lockID) {
    switch (type) {
      case LOCKED:
        assertKeyLiteral(key);
        this.tcObjectServerMap.doLogicalRemove(this, lockID, key);
        break;
      case UNLOCKED:
        this.tcObjectServerMap.doLogicalRemoveUnlocked(this, key);
    }
    if (metaData != null) {
      metaData.set(SearchConstants.Meta.COMMAND, SearchConstants.Commands.REMOVE);
      metaData.add(SearchConstants.Meta.KEY, key.toString());
      addMetaData(metaData);
    }
  }

  private SerializedMapValue createSerializedMapValue(final V value, int createTimeInSecs, int customMaxTTISeconds,
                                                      int customMaxTTLSeconds) {
    SerializedMapValueParameters<V> params = new SerializedMapValueParameters<V>();
    params.createTime(createTimeInSecs).deserialized(value).lastAccessedTime(createTimeInSecs);
    params.setCustomTTI(customMaxTTISeconds).setCustomTTL(customMaxTTLSeconds);

    params.serialized(strategy.serialize(value, compressionEnabled));

    return serializedClusterObjectFactory.createSerializedMapValue(params, gid);
  }

  private Object assertKeyLiteral(Object key) {
    if (!LiteralValues.isLiteralInstance(key)) {
      //
      throw new UnsupportedOperationException("Only literal keys are supported - key: " + key);
    }
    return key;
  }

  private static void beginLock(String lockID, final ToolkitLockTypeInternal type) {
    ManagerUtil.beginLock(lockID, LockingUtils.translate(type).toInt());
  }

  private static void commitLock(String lockID, final ToolkitLockTypeInternal type) {
    ManagerUtil.commitLock(lockID, LockingUtils.translate(type).toInt());
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(Object key) {
    final Long lockId = generateLockIdForKey(key);
    if (lockId == null) {
      //
      throw new UnsupportedOperationException("fine grained lock not supported with null lock for key [" + key + "]");
    }
    return new UnnamedToolkitReadWriteLock(lockId);
  }

  @Override
  public void clearLocalCache() {
    if (isEventual()) {
      // DEV-5244: no need to broadcast 'clear local cache' when invalidateOnChange
      internalClearLocalCache();
    } else {
      beginLock(getInstanceDsoLockName(), this.lockType);
      try {
        ManagerUtil.logicalInvoke(this, SerializationUtil.CLEAR_LOCAL_CACHE_SIGNATURE, NO_ARGS);
      } finally {
        commitLock(getInstanceDsoLockName(), this.lockType);
        ManagerUtil.waitForAllCurrentTransactionsToComplete();
        internalClearLocalCache();
      }
    }
  }

  private void internalClearLocalCache() {
    this.tcObjectServerMap.clearAllLocalCacheInline(this);
  }

  @Override
  public int localSize() {
    return this.tcObjectServerMap.getLocalSize();
  }

  @Override
  public Set<K> localKeySet() {
    return this.tcObjectServerMap.getLocalKeySet();
  }

  @Override
  public boolean containsLocalKey(final Object key) {
    return this.tcObjectServerMap.containsLocalKey(key);
  }

  @Override
  public void unpinAll() {
    if (!localCacheEnabled) { throw new UnsupportedOperationException(
                                                                      "unpinAll is not supported when local cache is disabled"); }
    this.tcObjectServerMap.unpinAll();
  }

  @Override
  public boolean isPinned(K key) {
    return this.tcObjectServerMap.isPinned(assertKeyLiteral(key));
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    if (!localCacheEnabled) { throw new UnsupportedOperationException(
                                                                      "Pinning is not supported when local cache is disabled"); }
    this.tcObjectServerMap.setPinned(assertKeyLiteral(key), pinned);
  }

  @Override
  public Set<K> keySet() {
    return new ServerMapKeySet<K, V>(this, tcObjectServerMap.keySet(this));
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new ServerMapEntrySet<K, V>(this, tcObjectServerMap.keySet(this));
  }

  private void assertNotNull(final Object value) {
    if (null == value) { throw new NullPointerException(); }
  }

  private void assertNull(final Object value) {
    if (null != value) { throw new AssertionError(); }
  }

  private boolean isExplicitlyLocked() {
    // TODO: fix this
    return false;
  }

  private UnsupportedOperationException newEventualExplicitLockedError() {
    // TODO: fix this with isExplictitlyLocked()
    return new UnsupportedOperationException("Eventual with explicit locking not supported yet");
  }

  @Override
  public V put(final K key, final V value) {
    return putWithMetaData(key, value, timeSource.nowInSeconds(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS,
                           ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS, null);
  }

  @Override
  public V putWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                           MetaData metaData) {

    assertNotNull(value);

    if (isEventual()) {
      if (isExplicitlyLocked()) {
        throw newEventualExplicitLockedError();
      } else {
        EVENTUAL_CONCURRENT_LOCK.lock();
        try {
          V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
          doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
          return old;
        } finally {
          EVENTUAL_CONCURRENT_LOCK.unlock();
        }
      }
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        doLogicalPutLocked(lockID, key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
        return old;
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public void unlockedPutNoReturnWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                              int customMaxTTLSeconds, MetaData metaData) {
    doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
  }

  @Override
  public void putNoReturnWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                      int customMaxTTLSeconds, MetaData metaData) {
    assertNotNull(value);

    if (isEventual()) {
      if (isExplicitlyLocked()) {
        throw newEventualExplicitLockedError();
      } else {
        EVENTUAL_CONCURRENT_LOCK.lock();
        try {
          doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
        } finally {
          EVENTUAL_CONCURRENT_LOCK.unlock();
        }
      }
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        doLogicalPutLocked(lockID, key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
      } finally {
        commitLock(lockID, this.lockType);
      }
    }

  }

  @Override
  public V putIfAbsent(final K key, final V value) {
    assertNotNull(value);
    return internalPutIfAbsent(key, value, timeSource.nowInSeconds(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS,
                               ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS, null);
  }

  @Override
  public V putIfAbsentWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                   int customMaxTTLSeconds, MetaData metaData) {
    assertNotNull(value);
    return internalPutIfAbsent(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
  }

  private V internalPutIfAbsent(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                                MetaData metaData) {

    if (isEventual()) {
      if (isExplicitlyLocked()) {
        throw newEventualExplicitLockedError();
      } else {
        EVENTUAL_CONCURRENT_LOCK.lock();
        try {
          SerializedMapValue serializedMapValue = createSerializedMapValue(value, createTimeInSecs,
                                                                           customMaxTTISeconds, customMaxTTLSeconds);
          V old = deserialize(key,
                              asSerializedMapValue(this.tcObjectServerMap
                                  .doLogicalPutIfAbsentUnlocked(this, assertKeyLiteral(key), serializedMapValue)));
          if (old == null && metaData != null) {
            metaData.set(SearchConstants.Meta.COMMAND, SearchConstants.Commands.PUT_IF_ABSENT);
            metaData.add(SearchConstants.Meta.VALUE, serializedMapValue.getObjectID());
            addMetaData(metaData);
          }
          return old;
        } finally {
          EVENTUAL_CONCURRENT_LOCK.unlock();
        }
      }
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old == null) {
          doLogicalPutLocked(lockID, key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
        }
        return old;
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  /**
   * implemented in ClusteredMapImpl and direct call to TCObjectServerMap, bypassed ServerMap for this operation
   */
  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(final Object key) {
    return removeWithMetaData(key, null);
  }

  @Override
  public V removeWithMetaData(final Object key, final MetaData metaData) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return null;
    }

    if (isEventual()) {
      V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
      if (old != null) {
        EVENTUAL_CONCURRENT_LOCK.lock();
        try {
          doLogicalRemoveUnlocked(key, metaData);
        } finally {
          EVENTUAL_CONCURRENT_LOCK.unlock();
        }
      }
      return old;
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null) {
          doLogicalRemoveLocked(key, metaData, lockID);
        }
        return old;
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    return removeWithMetaData(key, value, null);
  }

  @Override
  public boolean removeWithMetaData(Object key, Object value, final MetaData metaData) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return false;
    }

    // TODO: what about value if its not Serializable

    assertNotNull(value);
    if (isEventual()) {
      V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
      if (old != null && old.equals(value)) {
        EVENTUAL_CONCURRENT_LOCK.lock();
        try {
          doLogicalRemoveUnlocked(key, metaData);
        } finally {
          EVENTUAL_CONCURRENT_LOCK.unlock();
        }
        return true;
      } else {
        return false;
      }
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null && old.equals(value)) {
          doLogicalRemoveLocked(key, metaData, lockID);
          return true;
        } else {
          return false;
        }
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public void unlockedRemoveNoReturnWithMetaData(Object key, MetaData metaData) {
    doLogicalRemoveUnlocked(key, metaData);
  }

  @Override
  public void removeNoReturnWithMetaData(Object key, final MetaData metaData) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return;
    }

    if (isEventual()) {
      EVENTUAL_CONCURRENT_LOCK.lock();
      try {
        doLogicalRemoveUnlocked(key, metaData);
      } finally {
        EVENTUAL_CONCURRENT_LOCK.unlock();
      }
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        doLogicalRemoveLocked(key, metaData, lockID);
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public V replace(final K key, final V value) {
    assertNotNull(value);

    if (isEventual()) {
      final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
      if (old != null) {
        EVENTUAL_CONCURRENT_LOCK.lock();
        try {
          doLogicalPutUnlocked(key, value, timeSource.nowInSeconds(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS,
                               ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS, null);
        } finally {
          EVENTUAL_CONCURRENT_LOCK.unlock();
        }
      }
      return old;
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null) {
          doLogicalPutLocked(lockID, key, value, timeSource.nowInSeconds(),
                             ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS, ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS,
                             null);
        }
        return old;
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    assertNotNull(oldValue);
    assertNotNull(newValue);

    if (isEventual()) {
      final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
      if (old != null && old.equals(oldValue)) {
        EVENTUAL_CONCURRENT_LOCK.lock();
        try {
          doLogicalPutUnlocked(key, newValue, timeSource.nowInSeconds(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS,
                               ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS, null);
          return true;
        } finally {
          EVENTUAL_CONCURRENT_LOCK.unlock();
        }
      } else {
        return false;
      }
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null && old.equals(oldValue)) {
          doLogicalPutLocked(lockID, key, newValue, timeSource.nowInSeconds(),
                             ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS, ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS,
                             null);
          return true;
        } else {
          return false;
        }
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public void evictedInServer(boolean notifyEvicted, Object key) {
    internalRemoveFromLocalCache(key);
    if (notifyEvicted) {
      notifyElementEvicted((K) key);
    }
  }

  private void internalRemoveFromLocalCache(Object key) {
    tcObjectServerMap.evictedInServer(key);
  }

  private void notifyElementEvicted(K key) {
    for (ToolkitCacheListener<K> listener : listeners) {
      listener.onEviction(key);
    }
  }

  private void notifyElementExpired(K key) {
    for (ToolkitCacheListener<K> listener : listeners) {
      listener.onExpiration(key);
    }
  }

  @Override
  public void clear() {
    clearWithMetaData(null);
  }

  @Override
  public void unlockedClearWithMetaData(MetaData metaData) {
    tcObjectServerMap.doClear(this);
    if (metaData != null) {
      metaData.set(SearchConstants.Meta.COMMAND, SearchConstants.Commands.CLEAR);
      addMetaData(metaData);
    }
  }

  @Override
  public void clearWithMetaData(final MetaData metaData) {
    beginLock(getInstanceDsoLockName(), this.lockType);
    try {
      unlockedClearWithMetaData(metaData);
    } finally {
      commitLock(getInstanceDsoLockName(), this.lockType);
      ManagerUtil.waitForAllCurrentTransactionsToComplete();
      internalClearLocalCache();
    }
  }

  @Override
  public boolean containsKey(final Object key) {
    return get(key) != null;
  }

  @Override
  public boolean containsValue(final Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V get(final Object key) {
    return doGet(key, isEventual() ? GetType.UNLOCKED : GetType.LOCKED, false);
  }

  @Override
  public V get(Object key, boolean quiet) {
    return doGet(key, isEventual() ? GetType.UNLOCKED : GetType.LOCKED, quiet);
  }

  @Override
  public V unlockedGet(final K key, boolean quiet) {
    return doGet(key, GetType.UNLOCKED, quiet);
  }

  @Override
  public V unsafeLocalGet(final Object key) {
    return doGet(key, GetType.UNSAFE, true);
  }

  @Override
  public boolean isEmpty() {
    return (size() == 0);
  }

  @Override
  public int size() {
    long sum = ((TCObjectServerMap) __tc_managed()).getAllSize(new TCServerMap[] { this });
    // copy the way CHM does if overflow integer
    if (sum > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) sum;
    }
  }

  /**
   * Note this allows locking on keys that are not present in the map. Hibernate uses this to lock a key prior to
   * creating a mapping for it.
   */
  public void lockEntry(final K key) {
    final Long lockId = generateLockIdForKey(key);
    beginLock(lockId, this.lockType);
  }

  public void unlockEntry(final K key) {
    final Long lockId = generateLockIdForKey(key);
    commitLock(lockId, this.lockType);
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return tcObjectServerMap.getLocalOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return tcObjectServerMap.getLocalOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return tcObjectServerMap.getLocalOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return tcObjectServerMap.getLocalOffHeapSize();
  }

  protected void destroyLocalCache() {
    try {
      internalClearLocalCache();
    } finally {
      l1ServerMapLocalCacheStore = null;
      if (tcObjectServerMap != null) {
        tcObjectServerMap.destroyLocalStore();
      }
    }
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return tcObjectServerMap.containsKeyLocalOnHeap(assertKeyLiteral(key));
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return tcObjectServerMap.containsKeyLocalOffHeap(assertKeyLiteral(key));
  }

  private void setMaxEntriesLocalHeap(int maxEntriesLocalHeap) {
    if (l1ServerMapLocalCacheStore != null) {
      l1ServerMapLocalCacheStore.setMaxEntriesLocalHeap(maxEntriesLocalHeap);
    }
  }

  private void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
    if (l1ServerMapLocalCacheStore != null) {
      l1ServerMapLocalCacheStore.setMaxBytesLocalHeap(maxBytesLocalHeap);
    }
  }

  private void setLocalCacheEnabled(boolean enabled) {
    if (isLocalCacheEnabled() && !enabled) {
      tcObjectServerMap.clearAllLocalCacheInline(this);
    }

    tcObjectServerMap.setLocalCacheEnabled(enabled);
  }

  public void recalculateLocalCacheSize(Object key) {
    tcObjectServerMap.recalculateLocalCacheSize(assertKeyLiteral(key));
  }

  @Override
  public Collection<V> values() {
    if (values == null) {
      values = new AbstractCollection<V>() {
        @Override
        public Iterator<V> iterator() {
          return new Iterator<V>() {
            private final Iterator<Entry<K, V>> i = entrySet().iterator();

            @Override
            public boolean hasNext() {
              return i.hasNext();
            }

            @Override
            public V next() {
              return i.next().getValue();
            }

            @Override
            public void remove() {
              i.remove();
            }
          };
        }

        @Override
        public int size() {
          return ServerMap.this.size();
        }

        @Override
        public boolean contains(Object v) {
          return ServerMap.this.containsValue(v);
        }
      };
    }
    return values;
  }

  @Override
  public void addCacheListener(ToolkitCacheListener<K> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeCacheListener(ToolkitCacheListener<K> listener) {
    listeners.remove(listener);
  }

  @Override
  public void cleanupOnDestroy() {
    disposeLocally();
  }

  @Override
  public int getMaxTTISeconds() {
    return maxTTISeconds;
  }

  @Override
  public int getMaxTTLSeconds() {
    return maxTTLSeconds;
  }

  @Override
  public int getMaxCountInCluster() {
    return maxCountInCluster;
  }

  @Override
  public boolean isLocalCacheEnabled() {
    return localCacheEnabled;
  }

  public static enum GetType {
    LOCKED, UNLOCKED, UNSAFE;
  }

  private static enum MutateType {
    LOCKED, UNLOCKED;
  }

  @Override
  public void setConfigField(String name, Object value) {
    if (name.equals(ToolkitCacheConfigFields.MAX_TOTAL_COUNT_FIELD_NAME)) {
      setMaxTotalCount(((Integer) value).intValue());
    } else if (name.equals(ToolkitCacheConfigFields.MAX_TTI_SECONDS_FIELD_NAME)) {
      setMaxTTI(((Integer) value).intValue());
    } else if (name.equals(ToolkitCacheConfigFields.MAX_TTL_SECONDS_FIELD_NAME)) {
      setMaxTTL(((Integer) value).intValue());
    } else if (name.equals(ToolkitStoreConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME)) {
      setLocalCacheEnabled(((Boolean) value).booleanValue());
    } else if (name.equals(ToolkitStoreConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME)) {
      setMaxEntriesLocalHeap(((Integer) value).intValue());
    } else if (name.equals(ToolkitStoreConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME)) {
      setMaxBytesLocalHeap(((Long) value).longValue());
    } else {
      throw new IllegalArgumentException("ServerMap cannot set " + " name=" + name);
    }
  }

  private void setMaxTTI(int intValue) {
    try {
      this.maxTTISeconds = intValue;
      ManagerUtil.logicalInvoke(this, SerializationUtil.SET_MAX_TTI_SIGNATURE, new Object[] { this.maxTTISeconds });
    } finally {
      internalClearLocalCache();
    }
  }

  private void setMaxTTL(int intValue) {
    try {
      this.maxTTLSeconds = intValue;
      ManagerUtil.logicalInvoke(this, SerializationUtil.SET_MAX_TTL_SIGNATURE, new Object[] { this.maxTTLSeconds });
    } finally {
      internalClearLocalCache();
    }
  }

  private void setMaxTotalCount(int intValue) {
    try {
      this.maxCountInCluster = intValue;
      ManagerUtil.logicalInvoke(this, SerializationUtil.SET_TARGET_MAX_TOTAL_COUNT_SIGNATURE,
                                new Object[] { this.maxCountInCluster });
    } finally {
      internalClearLocalCache();
    }
  }

  @Override
  public void setMetaDataCallback(ToolkitCacheMetaDataCallback callback) {
    this.metaDataCallback = callback;
  }

  @Override
  public boolean isCompressionEnabled() {
    return compressionEnabled;
  }

  @Override
  public boolean isCopyOnReadEnabled() {
    return copyOnReadEnabled;
  }

  @Override
  public void disposeLocally() {
    try {
      internalClearLocalCache();
    } finally {
      tcObjectServerMap.destroyLocalStore();
      this.l1ServerMapLocalCacheStore.dispose();
      this.l1ServerMapLocalCacheStore = null;
    }
  }

}
