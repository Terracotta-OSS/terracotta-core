/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.rejoin.RejoinException;
import org.terracotta.toolkit.search.SearchException;
import org.terracotta.toolkit.search.attribute.NullAttributeExtractor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractorException;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeType;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

import com.google.common.base.Preconditions;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.properties.TCPropertiesConsts;
import com.terracotta.toolkit.TerracottaProperties;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.concurrent.locks.LockingUtils;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.meta.Extractor;
import com.terracotta.toolkit.meta.MetaData;
import com.terracotta.toolkit.meta.MetaDataImpl;
import com.terracotta.toolkit.meta.ToolkitCacheMetaDataCallback;
import com.terracotta.toolkit.object.AbstractTCToolkitObject;
import com.terracotta.toolkit.object.serialization.CustomLifespanSerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializedMapValueParameters;
import com.terracotta.toolkit.util.ExternalLockingWrapper;
import com.terracottatech.search.SearchCommand;
import com.terracottatech.search.SearchMetaData;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

public class ServerMap<K, V> extends AbstractTCToolkitObject implements InternalToolkitMap<K, V>, NotClearable {
  private static final TCLogger                                           LOGGER              = TCLogging
                                                                                                  .getLogger(ServerMap.class);
  private static final Object[]                                           NO_ARGS             = new Object[0];
  private final ToolkitLock                                               expireConcurrentLock;
  private final ToolkitLock                                               eventualConcurrentLock;

  private final boolean                                                   debugExpiration;

  // clustered fields
  private final ToolkitLockTypeInternal                                   lockType;
  private volatile boolean                                                localCacheEnabled;
  private volatile boolean                                                compressionEnabled;
  private volatile boolean                                                copyOnReadEnabled;
  private volatile int                                                    maxTTISeconds;
  private volatile int                                                    maxTTLSeconds;
  private volatile int                                                    maxCountInCluster;
  private volatile boolean                                                evictionEnabled;
  private volatile boolean                                                broadcastEvictions;

  // unclustered local fields
  protected volatile TCObjectServerMap<Long>                              tcObjectServerMap;
  protected volatile L1ServerMapLocalCacheStore                           l1ServerMapLocalCacheStore;
  protected volatile LongLockStrategy                                     lockStrategy;
  private volatile String                                                 instanceDsoLockName = null;
  private volatile CopyOnWriteArraySet<ToolkitCacheListener<K>>           listeners;
  private volatile Collection<V>                                          values              = null;
  private volatile TimeSource                                             timeSource;

  private final String                                                    name;
  private final Consistency                                               consistency;
  private final ToolkitCacheMetaDataCallback                              metaDataCallback;
  private final AtomicReference<ToolkitMap<String, ToolkitAttributeType>> attributeSchema     = new AtomicReference<ToolkitMap<String, ToolkitAttributeType>>();
  private volatile ToolkitAttributeExtractor                              attrExtractor       = ToolkitAttributeExtractor.NULL_EXTRACTOR;

  public ServerMap(Configuration config, String name, boolean broadcastEvictions) {
    this(config, name);
    this.broadcastEvictions = broadcastEvictions;
  }

  public ServerMap(Configuration config, String name) {
    this.name = name;

    this.expireConcurrentLock = ToolkitLockingApi
        .createConcurrentTransactionLock("servermap-static-expire-concurrent-lock", platformService);
    this.eventualConcurrentLock = ToolkitLockingApi
        .createConcurrentTransactionLock("servermap-static-eventual-concurrent-lock", platformService);
    this.debugExpiration = new TerracottaProperties(platformService).getBoolean("toolkit.map.expiration.debug", false);
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

    // eviction configuration doesn't exist for store
    final Serializable value = InternalCacheConfigurationType.EVICTION_ENABLED.getValueIfExists(config);
    this.evictionEnabled = (value == null) ? false : (Boolean) value;

    this.maxCountInCluster = (Integer) InternalCacheConfigurationType.MAX_TOTAL_COUNT.getValueIfExistsOrDefault(config);
    this.maxTTISeconds = (Integer) InternalCacheConfigurationType.MAX_TTI_SECONDS.getValueIfExistsOrDefault(config);
    this.maxTTLSeconds = (Integer) InternalCacheConfigurationType.MAX_TTL_SECONDS.getValueIfExistsOrDefault(config);

    this.listeners = new CopyOnWriteArraySet<ToolkitCacheListener<K>>();
    this.timeSource = new SystemTimeSource();
    this.compressionEnabled = (Boolean) InternalCacheConfigurationType.COMPRESSION_ENABLED
        .getExistingValueOrException(config);
    this.copyOnReadEnabled = (Boolean) InternalCacheConfigurationType.COPY_ON_READ_ENABLED
        .getExistingValueOrException(config);
    this.metaDataCallback = new ToolkitCacheMetaDataCallback() {

      @Override
      public MetaData getEvictRemoveMetaData() {
        if (!isSearchable()) return null;

        return createBaseMetaData();
      }
    };
  }

  @Override
  public void initializeLocalCache(L1ServerMapLocalCacheStore<K, V> localCacheStore, PinnedEntryFaultCallback callback,
                                   boolean localCacheEnabledParam) {
    if (localCacheStore == null) { throw new AssertionError("Local Cache Store cannot be null"); }
    this.localCacheEnabled = localCacheEnabledParam;
    this.l1ServerMapLocalCacheStore = localCacheStore;
    this.tcObjectServerMap.initialize(maxTTISeconds, maxTTLSeconds, maxCountInCluster, isEventual(),
                                      localCacheEnabledParam);
    this.tcObjectServerMap.setupLocalStore(l1ServerMapLocalCacheStore, callback);
  }

  @Override
  public void __tc_managed(TCObject t) {
    super.__tc_managed(t);
    if (!(t instanceof TCObjectServerMap)) { throw new AssertionError("Wrong tc object created for ServerMap - " + t); }
    // as cache won't go through RejoinAwarePlatformService, we make this proxy to
    // check that any operation that we do on cache, is not being done under a lock which was taken before rejoin:
    // dev-9033
    this.tcObjectServerMap = ExternalLockingWrapper
        .newExternalLockingProxy(TCObjectServerMap.class, platformService, t);
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
  public boolean invalidateOnChange() {
    return isEventual()
           || new TerracottaProperties(platformService)
               .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_INVALIDATE_STRONG_CACHE_ENABLED, true);
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

  private void commitLock(final Long lockId, final ToolkitLockTypeInternal type) {
    try {
      platformService.commitLock(lockId, LockingUtils.translate(type));
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  private void beginLock(final Long lockId, final ToolkitLockTypeInternal type) {
    try {
      platformService.beginLock(lockId, LockingUtils.translate(type));
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
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

    if (isUnsafeGet(getType)) {
      serializedMapValue = expireEntryIfNecessary(key, serializedMapValue, getType, quiet);
      // don't touch tc layer when doing unsafe reads
      return deserialize(key, serializedMapValue, true);
    }
    serializedMapValue = expireEntryIfNecessary(key, serializedMapValue, getType, quiet);
    return deserialize(key, serializedMapValue);
  }

  @Override
  public V checkAndGetNonExpiredValue(K key, Object value, GetType getType, boolean quiet) {
    SerializedMapValue serializedMapValue = asSerializedMapValue(value);
    return deserialize(key, expireEntryIfNecessary(key, serializedMapValue, getType, quiet), isUnsafeGet(getType));
  }

  private boolean isUnsafeGet(GetType getType) {
    return getType == GetType.UNSAFE;
  }

  private SerializedMapValue expireEntryIfNecessary(Object key, SerializedMapValue serializedMapValue, GetType getType,
                                                    boolean quiet) {
    if (serializedMapValue == null) return null;
    if (isExpirationEnabled() || serializedMapValue instanceof CustomLifespanSerializedMapValue) {
      // check for expiration
      int now = timeSource.nowInSeconds();
      final boolean expired;
      if (serializedMapValue.isExpired(now, maxTTISeconds, maxTTLSeconds)) {
        if (!isUnsafeGet(getType)) {
          boolean readLocked = createLockForKey(key).readLock().isHeldByCurrentThread();
          if (!readLocked) {
            expire(key, serializedMapValue, getType);
          }
        }
        expired = true;
        serializedMapValue = null;
      } else {
        if (!quiet && !isUnsafeGet(getType)) {
          markUsed(key, serializedMapValue, now);
        }
        expired = false;
      }
      if (debugExpiration) {
        LOGGER.info("Expiration debug - Key: " + key + ", now: " + now + ", maxTTISeconds: " + maxTTISeconds
                    + ", maxTTLSeconds: " + maxTTLSeconds + ", expired: " + expired + ", serializedMapValue: "
                    + serializedMapValue);
      }
    }
    return serializedMapValue;
  }

  private V deserialize(Object key, SerializedMapValue serializedMapValue) {
    return deserialize(key, serializedMapValue, false);
  }

  private V deserialize(Object key, SerializedMapValue serializedMapValue, boolean local) {
    if (serializedMapValue == null) { return null; }
    try {
      V deserialized = null;

      if (copyOnReadEnabled) {
        deserialized = (V) serializedMapValue.getDeserializedValueCopy(strategy, compressionEnabled, local);
      } else {
        deserialized = (V) serializedMapValue.getDeserializedValue(strategy, compressionEnabled,
                                                                   l1ServerMapLocalCacheStore, key, local);
      }
      return deserialized;
    } catch (Exception e) {
      // TODO: handle differently?
      throw new RuntimeException(e);
    }
  }

  private void markUsed(Object key, SerializedMapValue serializedMapValue, int usedAtTime) {
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
    if (configTTI == ToolkitConfigFields.NO_MAX_TTI_SECONDS) { return false; }
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
    // perform local get for unsafe operations
    V deserializedValue = deserialize(key, serializedMapValue, isUnsafeGet(getType));
    MetaData metaData = getEvictRemoveMetaData();

    if (getType == GetType.LOCKED) {

      // XXX - call below uses its own metadata; evict md not preserved!
      boolean removed = remove(key, deserializedValue);

      if (removed) {
        notify = true;
      } else {
        notify = false;
      }
    } else {
      expireConcurrentLock.lock();
      try {
        boolean mutated = this.tcObjectServerMap.doLogicalRemoveUnlocked(this, key, serializedMapValue);
        if (mutated && metaData != null) {
          metaData.set(SearchMetaData.COMMAND, SearchCommand.REMOVE_IF_VALUE_EQUAL);
          metaData.add("", 1);
          metaData.add("", key);
          metaData.add("", serializedMapValue.getObjectID());
          addMetaData(metaData);
        }
      } finally {
        expireConcurrentLock.unlock();
        notify = true;
      }
    }
    if (notify) {
      notifyElementExpired((K) key);
    }
  }

  private MetaData getEvictRemoveMetaData() {
    return metaDataCallback != null ? metaDataCallback.getEvictRemoveMetaData() : null;
  }

  private boolean isExpirationEnabled() {
    return maxTTISeconds != ToolkitConfigFields.NO_MAX_TTI_SECONDS
           || maxTTLSeconds != ToolkitConfigFields.NO_MAX_TTL_SECONDS;
  }

  /**
   * Throws NPE if metaData is null
   */
  private void addMetaData(MetaData metaData) {
    MetaDataDescriptor mdd = Extractor.extractInternalDescriptorFrom(platformService, metaData);
    tcObjectServerMap.addMetaData(mdd);
  }

  private Object doLogicalGetValueLocked(Object key, final Long lockID) {
    try {
      return this.tcObjectServerMap.getValue(this, lockID, key);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  private Object doLogicalGetValueUnlocked(Object key) {
    try {
      return this.tcObjectServerMap.getValueUnlocked(this, key);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  private void doLogicalPutLocked(final Long lockID, K key, final V value, int createTimeInSecs,
                                  int customMaxTTISeconds, int customMaxTTLSeconds, MetaData md) {
    doLogicalPut(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, MutateType.LOCKED, lockID, md);
  }

  private void doLogicalPutUnlocked(K key, final V value, int createTimeInSecs, int customMaxTTISeconds,
                                    int customMaxTTLSeconds, MetaData md) {
    doLogicalPut(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, MutateType.UNLOCKED, null, md);
  }

  private void doLogicalPut(K key, final V value, int createTimeInSecs, int customMaxTTISeconds,
                            int customMaxTTLSeconds, final MutateType type, final Long lockID, MetaData metaData) {
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
      metaData.add(SearchMetaData.VALUE, serializedMapValue.getObjectID());
      addMetaData(metaData);
    }
  }

  private void doLogicalRemoveLocked(Object key, final Long lockID) {
    doLogicalRemove(key, MutateType.LOCKED, lockID);
  }

  private void doLogicalRemoveUnlocked(Object key) {
    doLogicalRemove(key, MutateType.UNLOCKED, null);
  }

  private void doLogicalRemove(final Object key, MutateType type, Long lockID) {
    switch (type) {
      case LOCKED:
        assertKeyLiteral(key);
        this.tcObjectServerMap.doLogicalRemove(this, lockID, key);
        break;
      case UNLOCKED:
        this.tcObjectServerMap.doLogicalRemoveUnlocked(this, key);
    }
    MetaData metaData = createRemoveSearchMetaData(key);

    if (metaData != null) {
      metaData.set(SearchMetaData.COMMAND, SearchCommand.REMOVE);
      metaData.add(SearchMetaData.KEY, key.toString());
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

  private void beginLock(String lockID, final ToolkitLockTypeInternal type) {
    ToolkitLockingApi.lock(lockID, type, platformService);
  }

  private void commitLock(String lockID, final ToolkitLockTypeInternal type) {
    ToolkitLockingApi.unlock(lockID, type, platformService);
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(Object key) {
    final Long lockId = generateLockIdForKey(key);
    if (lockId == null) {
      //
      throw new UnsupportedOperationException("fine grained lock not supported with null lock for key [" + key + "]");
    }
    return ToolkitLockingApi.createUnnamedReadWriteLock(lockId, platformService, lockType);
  }

  @Override
  public void cleanLocalState() {
    this.tcObjectServerMap.cleanLocalState();
  }

  @Override
  public void clearLocalCache() {
    if (isEventual()) {
      // DEV-5244: no need to broadcast 'clear local cache' when invalidateOnChange
      internalClearLocalCache();
    } else {
      beginLock(getInstanceDsoLockName(), this.lockType);
      try {
        platformService.logicalInvoke(this, SerializationUtil.CLEAR_LOCAL_CACHE_SIGNATURE, NO_ARGS);
      } finally {
        commitLock(getInstanceDsoLockName(), this.lockType);
        try {
          platformService.waitForAllCurrentTransactionsToComplete();
        } catch (AbortedOperationException e) {
          throw new ToolkitAbortableOperationException(e);
        }
        internalClearLocalCache();
      }
    }
  }

  private void internalClearLocalCache() {
    this.tcObjectServerMap.clearAllLocalCacheInline();
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
  public Set<K> keySet() {
    Set keySet = null;
    try {
      keySet = tcObjectServerMap.keySet(this);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
    return new ServerMapKeySet<K, V>(this, keySet);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set keySet = null;
    try {
      keySet = tcObjectServerMap.keySet(this);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
    return new ServerMapEntrySet<K, V>(this, keySet);
  }

  private void assertNotNull(final Object value) {
    Preconditions.checkNotNull(value);
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
    return put(key, value, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
               ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    assertNotNull(value);
    throttleIfNecessary();

    if (isEventual()) {
      if (isExplicitlyLocked()) {
        throw newEventualExplicitLockedError();
      } else {

        // Do this outside the lock
        // NOTE - pass to extractor original value, not serialized version
        MetaData metaData = createPutSearchMetaData(key, value);
        if (metaData != null) {
          metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
        }

        eventualConcurrentLock.lock();
        try {
          V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
          doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
          return old;
        } finally {
          eventualConcurrentLock.unlock();
        }
      }
    } else {
      MetaData metaData = createPutSearchMetaData(key, value);

      if (metaData != null) {
        metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
      }

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
  public void unlockedPutNoReturn(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    throttleIfNecessary();
    MetaData md = createPutSearchMetaData(key, value);
    if (md != null) {
      md.set(SearchMetaData.COMMAND, SearchCommand.PUT);

    }
    doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, md);
  }

  @Override
  public void putNoReturn(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    assertNotNull(value);
    throttleIfNecessary();

    if (isEventual()) {
      if (isExplicitlyLocked()) {
        throw newEventualExplicitLockedError();
      } else {
        MetaData metaData = createPutSearchMetaData(key, value);
        if (metaData != null) {
          metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
        }
        eventualConcurrentLock.lock();
        try {
          doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
        } finally {
          eventualConcurrentLock.unlock();
        }
      }
    } else {
      MetaData metaData = createPutSearchMetaData(key, value);
      if (metaData != null) {
        metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
      }

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
    throttleIfNecessary();
    return internalPutIfAbsent(key, value, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                               ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public V putIfAbsent(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    assertNotNull(value);
    throttleIfNecessary();
    return internalPutIfAbsent(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  private V internalPutIfAbsent(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {

    if (isEventual()) {
      if (isExplicitlyLocked()) {
        throw newEventualExplicitLockedError();
      } else {
        K portableKey = (K) assertKeyLiteral(key);
        MetaData metaData = createPutSearchMetaData(portableKey, value);

        eventualConcurrentLock.lock();
        try {
          SerializedMapValue serializedMapValue = createSerializedMapValue(value, createTimeInSecs,
                                                                           customMaxTTISeconds, customMaxTTLSeconds);
          V old = deserialize(key,
                              asSerializedMapValue(this.tcObjectServerMap
                                  .doLogicalPutIfAbsentUnlocked(this, portableKey, serializedMapValue)));
          if (old == null && metaData != null) {
            metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT_IF_ABSENT);
            metaData.add(SearchMetaData.VALUE, serializedMapValue.getObjectID());
            addMetaData(metaData);
          }
          return old;
        } finally {
          eventualConcurrentLock.unlock();
        }
      }
    } else {

      MetaData metaData = createPutSearchMetaData(key, value);

      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old == null) {
          if (metaData != null) {
            metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT_IF_ABSENT);
          }

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
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return null;
    }
    if (isEventual()) {
      V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
      if (old != null) {
        eventualConcurrentLock.lock();
        try {
          doLogicalRemoveUnlocked(key);
        } finally {
          eventualConcurrentLock.unlock();
        }
      }
      return old;
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null) {
          doLogicalRemoveLocked(key, lockID);
        }
        return old;
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return false;
    }

    // TODO: what about value if its not Serializable

    assertNotNull(value);
    if (isEventual()) {
      V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
      if (old != null && old.equals(value)) {
        eventualConcurrentLock.lock();
        try {
          doLogicalRemoveUnlocked(key);
        } finally {
          eventualConcurrentLock.unlock();
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
          doLogicalRemoveLocked(key, lockID);
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
  public void unlockedRemoveNoReturn(Object key) {
    doLogicalRemoveUnlocked(key);
  }

  @Override
  public void removeNoReturn(Object key) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return;
    }

    if (isEventual()) {
      eventualConcurrentLock.lock();
      try {
        doLogicalRemoveUnlocked(key);
      } finally {
        eventualConcurrentLock.unlock();
      }
    } else {
      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        doLogicalRemoveLocked(key, lockID);
      } finally {
        commitLock(lockID, this.lockType);
      }
    }
  }

  @Override
  public V replace(final K key, final V value) {
    assertNotNull(value);

    throttleIfNecessary();
    if (isEventual()) {
      final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
      if (old != null) {
        MetaData metaData = createPutSearchMetaData(key, value);
        if (metaData != null) {
          metaData.set(SearchMetaData.COMMAND, SearchCommand.REPLACE);
        }
        eventualConcurrentLock.lock();
        SerializedMapValue newSerializedMapValue = createSerializedMapValue(value, timeSource.nowInSeconds(),
                                                                            ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                                                                            ToolkitConfigFields.NO_MAX_TTL_SECONDS);
        try {
          this.tcObjectServerMap.doLogicalReplaceUnlocked(this, key, newSerializedMapValue);
        } finally {
          eventualConcurrentLock.unlock();
        }
      }
      return old;
    } else {

      MetaData metaData = createPutSearchMetaData(key, value);
      if (metaData != null) {
        metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
      }

      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null) {
          doLogicalPut(key, value, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                       ToolkitConfigFields.NO_MAX_TTL_SECONDS, MutateType.LOCKED, lockID, metaData);
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
    throttleIfNecessary();

    if (isEventual()) {
      SerializedMapValue<V> oldSerializedMapValue = asSerializedMapValue(doLogicalGetValueUnlocked(key));
      final V old = deserialize(key, oldSerializedMapValue);

      MetaData metaData = createPutSearchMetaData(key, newValue);
      if (metaData != null) metaData.set(SearchMetaData.COMMAND, SearchCommand.REPLACE);

      if (old != null && old.equals(oldValue)) {
        eventualConcurrentLock.lock();
        try {
          SerializedMapValue newSerializedMapValue = createSerializedMapValue(newValue, timeSource.nowInSeconds(),
                                                                              ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                                                                              ToolkitConfigFields.NO_MAX_TTL_SECONDS);

          this.tcObjectServerMap.doLogicalReplaceUnlocked(this, key, oldSerializedMapValue, newSerializedMapValue);
          return true;
        } finally {
          eventualConcurrentLock.unlock();
        }
      } else {
        return false;
      }
    } else {
      MetaData metaData = createPutSearchMetaData(key, newValue);
      if (metaData != null) {
        metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
      }

      final Long lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null && old.equals(oldValue)) {

          doLogicalPut(key, newValue, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                       ToolkitConfigFields.NO_MAX_TTL_SECONDS, MutateType.LOCKED, lockID, metaData);
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
  public void unlockedClear() {
    tcObjectServerMap.doClear(this);
    MetaData metaData = createClearSearchMetaData();
    if (metaData != null) {
      metaData.set(SearchMetaData.COMMAND, SearchCommand.CLEAR);
      addMetaData(metaData);
    }
  }

  @Override
  public void clear() {
    beginLock(getInstanceDsoLockName(), this.lockType);
    try {
      unlockedClear();
    } finally {
      commitLock(getInstanceDsoLockName(), this.lockType);
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
    return size() == 0;
  }

  @Override
  public int size() {
    long sum = 0;
    try {
      sum = ((TCObjectServerMap) __tc_managed()).getAllSize(new TCServerMap[] { this });
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
    // copy the way CHM does if overflow integer
    if (sum > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) sum;
    }
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
      tcObjectServerMap.clearAllLocalCacheInline();
    }

    tcObjectServerMap.setLocalCacheEnabled(enabled);
  }

  void setSearchAttributeTypes(ToolkitMap<String, ToolkitAttributeType> schema) {
    if (!this.attributeSchema.compareAndSet(null, schema) && schema != attributeSchema.get()) {
      LOGGER.warn(String.format("Ignoring attempt to reset search attribute schema on map %s", getName()));
    }
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

  // This field might not be up-to-date from the value in cluster.
  @Override
  public int getMaxCountInCluster() {
    return maxCountInCluster;
  }

  @Override
  public boolean isLocalCacheEnabled() {
    return localCacheEnabled;
  }

  public enum GetType {
    LOCKED, UNLOCKED, UNSAFE
  }

  private enum MutateType {
    LOCKED, UNLOCKED
  }

  @Override
  public void setConfigField(String name, Object value) {
    if (ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME.equals(name)) {
      setMaxTotalCount((Integer) value);
    } else if (ToolkitConfigFields.EVICTION_ENABLED_FIELD_NAME.equals(name)) {
      setEvictionEnabled((Boolean) value);
    } else if (ToolkitConfigFields.MAX_TTI_SECONDS_FIELD_NAME.equals(name)) {
      setMaxTTI((Integer) value);
    } else if (ToolkitConfigFields.MAX_TTL_SECONDS_FIELD_NAME.equals(name)) {
      setMaxTTL((Integer) value);
    } else if (ToolkitConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME.equals(name)) {
      setLocalCacheEnabled((Boolean) value);
    } else if (ToolkitConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME.equals(name)) {
      setMaxEntriesLocalHeap((Integer) value);
    } else if (ToolkitConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME.equals(name)) {
      setMaxBytesLocalHeap((Long) value);
    } else {
      throw new IllegalArgumentException("ServerMap cannot set " + " name=" + name);
    }
  }

  @Override
  public void setConfigFieldInternal(String name, Object value) {
    if (ToolkitConfigFields.EVICTION_ENABLED_FIELD_NAME.equals(name)) {
      this.evictionEnabled = (Boolean) value;
    } else if (ToolkitConfigFields.MAX_TTI_SECONDS_FIELD_NAME.equals(name)) {
      this.maxTTISeconds = (Integer) value;
    } else if (ToolkitConfigFields.MAX_TTL_SECONDS_FIELD_NAME.equals(name)) {
      this.maxTTLSeconds = (Integer) value;
    }
  }

  private void setMaxTTI(int intValue) {
    try {
      this.maxTTISeconds = intValue;
      platformService.logicalInvoke(this, SerializationUtil.INT_FIELD_CHANGED_SIGNATURE, new Object[] {
          ServerMapApplicator.MAX_TTI_SECONDS_FIELDNAME, this.maxTTISeconds });
    } finally {
      internalClearLocalCache();
    }
  }

  private void setMaxTTL(int intValue) {
    try {
      this.maxTTLSeconds = intValue;
      platformService.logicalInvoke(this, SerializationUtil.INT_FIELD_CHANGED_SIGNATURE, new Object[] {
          ServerMapApplicator.MAX_TTL_SECONDS_FIELDNAME, this.maxTTLSeconds });
    } finally {
      internalClearLocalCache();
    }
  }

  private void setMaxTotalCount(int intValue) {
    try {
      this.maxCountInCluster = intValue;
      platformService.logicalInvoke(this, SerializationUtil.INT_FIELD_CHANGED_SIGNATURE, new Object[] {
          ServerMapApplicator.MAX_COUNT_IN_CLUSTER_FIELDNAME, this.maxCountInCluster });
    } finally {
      internalClearLocalCache();
    }
  }

  @Override
  public boolean isEvictionEnabled() {
    return evictionEnabled;
  }

  private void setEvictionEnabled(boolean value) {
    if (this.evictionEnabled != value) {
      this.evictionEnabled = value;
      platformService.logicalInvoke(this, SerializationUtil.FIELD_CHANGED_SIGNATURE, new Object[] {
          ServerMapApplicator.EVICTION_ENABLED_FIELDNAME, this.evictionEnabled });
    }
  }

  @Override
  public void setBroadcastEvictions(boolean broadcastEvictions) {
    if (this.broadcastEvictions != broadcastEvictions) {
      this.broadcastEvictions = broadcastEvictions;
      platformService.logicalInvoke(this, SerializationUtil.FIELD_CHANGED_SIGNATURE, new Object[] {
          ServerMapApplicator.BROADCAST_EVICTIONS_FIELDNAME, this.broadcastEvictions });
    }
  }

  public boolean isBroadcastEvictions() {
    return broadcastEvictions;
  }

  private boolean isSearchable() {
    return attrExtractor != null && !(attrExtractor instanceof NullAttributeExtractor);
  }

  private String getAttrTypeMapLockName() {
    return name + "|attrTypeMapLock:";
  }

  private MetaData createRemoveSearchMetaData(Object key) {
    return isSearchable() ? createBaseMetaData() : null;
  }

  private MetaData createClearSearchMetaData() {
    return createRemoveSearchMetaData(null);
  }

  private MetaData createPutSearchMetaData(K key, V value) {
    if (!isSearchable()) return null;

    MetaData md = createBaseMetaData();
    md.add(SearchMetaData.KEY, key);

    try {
      Map<String, ToolkitAttributeType> recordedTypes = new HashMap<String, ToolkitAttributeType>();
      Map<String, ToolkitAttributeType> searchAttributeTypes = attributeSchema.get();
      boolean updateTypes = searchAttributeTypes.isEmpty();

      Map<String, Object> attrs = attrExtractor.attributesFor(key, value);
      if (attrs == ToolkitAttributeExtractor.DO_NOT_INDEX) return null;
      for (Map.Entry<String, Object> attr : attrs.entrySet()) {
        String attrName = attr.getKey();
        Object attrValue = attr.getValue();

        if (attrValue != null) {
          if (updateTypes) {
            recordedTypes.put(attrName, ToolkitAttributeType.typeFor(attrName, attrValue));
          } else {
            ToolkitAttributeType type = searchAttributeTypes.get(attrName);
            if (type == null) {
              recordedTypes.put(attrName, ToolkitAttributeType.typeFor(attrName, attrValue));
            } else {
              type.validateValue(attrName, attrValue);
            }
          }

          md.add(SearchMetaData.ATTR + attr.getKey(), attr.getValue());
        }
      }

      if (!recordedTypes.isEmpty()) {
        // check that we're overwriting any existing entries
        String lockId = getInstanceDsoLockName() + getAttrTypeMapLockName();
        try {
          beginLock(lockId, ToolkitLockTypeInternal.WRITE);
          for (Entry<String, ToolkitAttributeType> e : recordedTypes.entrySet()) {
            String attrName = e.getKey();
            ToolkitAttributeType existing = searchAttributeTypes.get(attrName);
            ToolkitAttributeType newType = e.getValue();
            if (existing != null && existing != newType) {
              //
              throw new SearchException("Attempting to replace type mapping for attribute [" + attrName + "] from "
                                        + existing + " to " + newType);
            }
          }
          searchAttributeTypes.putAll(recordedTypes);
        } finally {
          commitLock(lockId, ToolkitLockTypeInternal.WRITE);
        }
      }
    } catch (ToolkitAttributeExtractorException e) {
      LOGGER.warn(String.format("Error extracting search attributes from key %s and value %s", key, value), e);
      throw e;
    }

    return md;
  }

  private MetaData createMetaData(String category) {
    return new MetaDataImpl(platformService, category);
  }

  private MetaData createBaseMetaData() {
    MetaData meta = createMetaData("SEARCH");
    // TODO: does this match Ehcache's fully qualified name?
    meta.add(SearchMetaData.CACHENAME, name);
    meta.add(SearchMetaData.COMMAND, SearchCommand.NOT_SET);
    return meta;
  }

  private void throttleIfNecessary() throws ToolkitAbortableOperationException {
    try {
      platformService.throttlePutIfNecessary(tcObjectServerMap.getObjectID());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
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

  @Override
  public void registerAttributeExtractor(ToolkitAttributeExtractor extractor) {
    this.attrExtractor = extractor;
  }

  private static class LongLockStrategy<K> {
    private final long highBits;

    public LongLockStrategy(String instanceQualifier) {
      this.highBits = ((long) instanceQualifier.hashCode()) << 32;
    }

    public Long generateLockIdForKey(K key) {
      long lowBits = key.hashCode() & 0x00000000FFFFFFFFL;
      return highBits | lowBits;
    }
  }

}
