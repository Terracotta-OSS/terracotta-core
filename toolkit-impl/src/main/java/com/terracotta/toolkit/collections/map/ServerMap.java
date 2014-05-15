/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import static com.tc.server.VersionedServerEvent.DEFAULT_VERSION;

import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal.LOCK_STRATEGY;
import org.terracotta.toolkit.rejoin.RejoinException;
import org.terracotta.toolkit.search.SearchException;
import org.terracotta.toolkit.search.attribute.NullAttributeExtractor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractorException;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeType;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LiteralValues;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TCObjectServerMap;
import com.tc.object.VersionedObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.properties.TCPropertiesConsts;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.terracotta.toolkit.TerracottaProperties;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.bulkload.BufferedOperation;
import com.terracotta.toolkit.bulkload.LocalBufferedMap;
import com.terracotta.toolkit.concurrent.locks.LockStrategy;
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
import com.terracotta.toolkit.util.ExplicitLockingTCObjectServerMapImpl;
import com.terracottatech.search.SearchCommand;
import com.terracottatech.search.SearchMetaData;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ServerMap<K, V> extends AbstractTCToolkitObject implements InternalToolkitMap<K, V> {
  private static final TCLogger                             LOGGER              = TCLogging.getLogger(ServerMap.class);
  private static final Object[]                             NO_ARGS             = new Object[0];
  private static final String                               LOCK_PREFIX         = "__servermap@lock-";
  private static final String                               KEY_LOCK_PREFIX     = LOCK_PREFIX + "key-";

  private final ToolkitLock                                 expireConcurrentLock;
  private final ToolkitLock                                 eventualConcurrentLock;

  private final boolean                                     debugExpiration;

  // clustered fields
  private final ToolkitLockTypeInternal                     lockType;
  private volatile boolean                                  localCacheEnabled;
  private volatile boolean                                  compressionEnabled;
  private volatile boolean                                  copyOnReadEnabled;
  private volatile int                                      maxTTISeconds;
  private volatile int                                      maxTTLSeconds;
  private volatile int                                      maxCountInCluster;
  private volatile boolean                                  evictionEnabled;

  // unclustered local fields
  protected volatile TCObjectServerMap<Object>              tcObjectServerMap;
  protected volatile L1ServerMapLocalCacheStore             l1ServerMapLocalCacheStore;
  protected volatile LockStrategy                           lockStrategy;
  private volatile String                                   instanceDsoLockName = null;
  private volatile TimeSource                               timeSource;
  private final String                                      name;
  private final Consistency                                 consistency;
  private final ToolkitCacheMetaDataCallback                metaDataCallback;
  private final AtomicReference<ToolkitMap<String, String>> attributeSchema     = new AtomicReference<ToolkitMap<String, String>>();
  private volatile ToolkitAttributeExtractor                attrExtractor       = ToolkitAttributeExtractor.NULL_EXTRACTOR;

  public ServerMap(Configuration config, String name) {
    this.name = name;
    this.expireConcurrentLock = ToolkitLockingApi
        .createConcurrentTransactionLock("servermap-static-expire-concurrent-lock", platformService);
    this.eventualConcurrentLock = ToolkitLockingApi
        .createConcurrentTransactionLock("servermap-static-eventual-concurrent-lock", platformService);
    this.debugExpiration = new TerracottaProperties(platformService).getBoolean("servermap.expiration.debug", false);
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
    // TODO: ServerMap should talk to PlatformService
    // DEV-9033 ServerMap doesn't talk to PlatformService, we need this wrapper to handle lock + rejoin scenario
    this.tcObjectServerMap = new ExplicitLockingTCObjectServerMapImpl((TCObjectServerMap) t, platformService);
  }

  @Override
  public void setLockStrategy(LOCK_STRATEGY strategy) {
    switch (strategy) {
      case LONG_LOCK_STRATEGY:
        lockStrategy = new LongLockStrategy(getInstanceDsoLockName());
        break;
      case STRING_LOCK_STRATEGY:
        lockStrategy = new StringLockStrategy();
        break;
      case OBJECT_LOCK_STRATEGY:
        lockStrategy = new ObjectLockStrategy();
        break;
      default:
        throw new IllegalArgumentException("unknown LockStrategy " + strategy);
    }
  }

  // Internal methods used by applicator
  @Override
  public ToolkitLockTypeInternal getLockType() {
    return lockType;
  }

  @Override
  public boolean isEventual() {
    // if explicitly locked then consider as strong
    return this.consistency == Consistency.EVENTUAL && !platformService.isExplicitlyLocked();
  }

  private ToolkitLockTypeInternal getEffectiveLockType() {
    if (this.lockType == ToolkitLockTypeInternal.CONCURRENT && platformService.isExplicitlyLocked()) {
      return ToolkitLockTypeInternal.WRITE;
    } else {
      return lockType;
    }
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

  private Object generateLockIdForKey(final Object key) {
    return lockStrategy.generateLockIdForKey(key);
  }

  private String getInstanceDsoLockName() {
    if (this.instanceDsoLockName != null) { return this.instanceDsoLockName; }

    // The trailing colon (':') is relevant to avoid unintended lock collision when appending object ID to the key
    // Without the delimiter you'd get the same effective lock for oid 11, lockId 10 and oid 1 and lockId 110
    this.instanceDsoLockName = "@ServerMap:" + ((Manageable) this).__tc_managed().getObjectID().toLong() + ":";
    return this.instanceDsoLockName;
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
        final Object lockID = generateLockIdForKey(key);
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
        Object value = doLogicalGetValueUnlocked(key);
        serializedMapValue = asSerializedMapValue(value);
        break;
      case UNSAFE:
        // use only local cache to look up value
        serializedMapValue = asSerializedMapValue(this.tcObjectServerMap.getValueFromLocalCache(key));
        break;
    }
    return serializedMapValue;
  }

  private SerializedMapValue asSerializedMapValue(Object obj) {
    if (obj != null) {
      if (obj instanceof SerializedMapValue) {
        return (SerializedMapValue) obj;
      } else {
        throw new AssertionError("Value is not instanceof SerializedMapValue: " + obj);
      }
    }
    return null;
  }

  private V getNonExpiredValue(Object key, SerializedMapValue serializedMapValue, GetType getType, boolean quiet) {
    if (serializedMapValue == null) { return null; }
    serializedMapValue = expireEntryIfNecessary(key, serializedMapValue, getType, quiet);
    if (isUnsafeGet(getType)) {
      // don't touch tc layer when doing unsafe reads
      return deserialize(key, serializedMapValue, true);
    }
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
      final V deserialized;
      if (copyOnReadEnabled) {
        deserialized = (V) serializedMapValue.getDeserializedValueCopy(serStrategy, compressionEnabled, local);
      } else {
        deserialized = (V) serializedMapValue.getDeserializedValue(serStrategy, compressionEnabled,
                                                                   l1ServerMapLocalCacheStore, key, local);
      }
      return deserialized;
    } catch (RejoinException e) {
      throw e;
    } catch (ToolkitAbortableOperationException e) {
      throw e;
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
    final MetaData metaData = getEvictRemoveMetaData();
    if (getType == GetType.LOCKED) {
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, this.lockType);
      try {
        doLogicalExpireLocked(lockID, key, serializedMapValue);
      } finally {
        commitLock(lockID, this.lockType);
      }
    } else {
      expireConcurrentLock.lock();
      try {
        boolean mutated = this.tcObjectServerMap.doLogicalExpireUnlocked(this, key, serializedMapValue);
        if (mutated && metaData != null) {
          metaData.set(SearchMetaData.COMMAND, SearchCommand.REMOVE_IF_VALUE_EQUAL);
          metaData.add("", 1);
          metaData.add("", key);
          metaData.add("", serializedMapValue.getObjectID());
          addMetaData(metaData);
        }
      } finally {
        expireConcurrentLock.unlock();
      }
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

  private MetaDataDescriptor getMetaDataDescriptor(MetaData metaData) {
    return Extractor.extractInternalDescriptorFrom(platformService, metaData);
  }

  private Object doLogicalGetValueLocked(Object key, final Object lockID) {
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

  private VersionedObject doLogicalGetVersionedValue(Object key) {
    try {
      return this.tcObjectServerMap.getVersionedValue(this, key);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  private void doLogicalPutLocked(final Object lockID, K key, final V value, int createTimeInSecs,
                                  int customMaxTTISeconds, int customMaxTTLSeconds, MetaData md) {
    doLogicalPut(key, value, DEFAULT_VERSION, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds,
                 MutateType.LOCKED, lockID, md);
  }

  private void doLogicalPutUnlocked(K key, final V value, int createTimeInSecs, int customMaxTTISeconds,
                                    int customMaxTTLSeconds, MetaData md) {
    doLogicalPut(key, value, DEFAULT_VERSION, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds,
                 MutateType.UNLOCKED, null, md);
  }

  private void doLogicalPut(final K key, final V value, int createTimeInSecs, int customMaxTTISeconds,
                            int customMaxTTLSeconds, final MutateType type, final Object lockID, final MetaData metaData) {
    doLogicalPut(key, value, DEFAULT_VERSION, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, type, lockID,
                 metaData);
  }

  private void doLogicalPut(final K key, final V value, final long version, int createTimeInSecs,
                            int customMaxTTISeconds, int customMaxTTLSeconds, final MutateType type,
                            final Object lockID, final MetaData metaData) {
    final K portableKey = assertKeyLiteral(key);
    final SerializedMapValue<V> serializedMapValue = createSerializedMapValue(value, createTimeInSecs,
                                                                           customMaxTTISeconds, customMaxTTLSeconds);

    doLogicalPut(version, type, lockID, metaData, portableKey, serializedMapValue);
  }

  private void doLogicalPut(final long version, final MutateType type, final Object lockID, final MetaData metaData, final K portableKey, final SerializedMapValue<V> serializedMapValue) {
    switch (type) {
      case LOCKED:
        assertNotNull(lockID);
        if (isVersioned(version)) {
          this.tcObjectServerMap.doLogicalPutVersioned(this, lockID, portableKey, serializedMapValue, version);
        } else {
          this.tcObjectServerMap.doLogicalPut(lockID, portableKey, serializedMapValue);
        }
        break;
      case UNLOCKED:
        assertNull(lockID);
        if (isVersioned(version)) {
          this.tcObjectServerMap.doLogicalPutUnlockedVersioned(this, portableKey, serializedMapValue, version);
        } else {
          this.tcObjectServerMap.doLogicalPutUnlocked(this, portableKey, serializedMapValue);
        }
        break;
    }
    if (metaData != null) {
      metaData.add(SearchMetaData.VALUE, serializedMapValue.getObjectID());
      addMetaData(metaData);
    }
  }

  private static boolean isVersioned(long version) {
    return version != DEFAULT_VERSION;
  }

  private void doLogicalRemoveLocked(Object key, final Object lockID) {
    internalLogicalRemove(key, DEFAULT_VERSION, MutateType.LOCKED, lockID);
  }

  private boolean doLogicalRemoveUnlocked(Object key, Object value) {
    try {
      MetaDataDescriptor mdd = null;
      MetaData metaData = createRemoveSearchMetaData(key);
      if (metaData != null) {
        metaData.set(SearchMetaData.COMMAND, SearchCommand.REMOVE);
        metaData.add(SearchMetaData.KEY, key.toString());
        mdd = getMetaDataDescriptor(metaData);
      }
      return this.tcObjectServerMap.doLogicalRemoveUnlocked(this, key, value, mdd);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException();
    }
  }

  private void doLogicalRemoveUnlocked(Object key) {
    internalLogicalRemove(key, DEFAULT_VERSION, MutateType.UNLOCKED, null);
  }

  private void doLogicalRemoveUnlockedVersioned(final Object key, final long version) {
    internalLogicalRemove(key, version, MutateType.UNLOCKED, null);
  }

  private void internalLogicalRemove(final Object key, final long version, MutateType type, Object lockID) {
    switch (type) {
      case LOCKED:
        assertKeyLiteral(key);
        if (isVersioned(version)) {
          this.tcObjectServerMap.doLogicalRemoveVersioned(this, lockID, key, version);
        } else {
          this.tcObjectServerMap.doLogicalRemove(this, lockID, key);
        }
        break;
      case UNLOCKED:
        if (isVersioned(version)) {
          this.tcObjectServerMap.doLogicalRemoveUnlockedVersioned(this, key, version);
        } else {
          this.tcObjectServerMap.doLogicalRemoveUnlocked(this, key);
        }
    }

    MetaData metaData = createRemoveSearchMetaData(key);
    if (metaData != null) {
      metaData.set(SearchMetaData.COMMAND, SearchCommand.REMOVE);
      metaData.add(SearchMetaData.KEY, key.toString());
      addMetaData(metaData);
    }
  }

  private void doLogicalExpireLocked(final Object lockID, final Object key, final Object value) {
    assertKeyLiteral(key);
    this.tcObjectServerMap.doLogicalExpire(lockID, key, value);

    MetaData metaData = createRemoveSearchMetaData(key);
    if (metaData != null) {
      metaData.set(SearchMetaData.COMMAND, SearchCommand.REMOVE);
      metaData.add(SearchMetaData.KEY, key.toString());
      addMetaData(metaData);
    }
  }

  private SerializedMapValue<V> createSerializedMapValue(final V value, int createTimeInSecs, int customMaxTTISeconds,
                                                      int customMaxTTLSeconds) {
    SerializedMapValueParameters<V> params = createSerializedMapValueParameters(value, createTimeInSecs,
        customMaxTTISeconds, customMaxTTLSeconds);

    return serializedClusterObjectFactory.createSerializedMapValue(params, gid);
  }

  private <T> SerializedMapValueParameters<T> createSerializedMapValueParameters(final T value, final int createTimeInSecs, final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    SerializedMapValueParameters<T> params = new SerializedMapValueParameters<T>();
    params.createTime(createTimeInSecs).deserialized(value).lastAccessedTime(createTimeInSecs);
    params.setCustomTTI(customMaxTTISeconds).setCustomTTL(customMaxTTLSeconds);

    params.serialized(serStrategy.serialize(value, compressionEnabled));
    return params;
  }

  private <T> T assertKeyLiteral(T key) {
    if (!LiteralValues.isLiteralInstance(key)) {
      //
      throw new UnsupportedOperationException("Only literal keys are supported - key: " + key);
    }
    return key;
  }

  private void beginLock(Object lockId, final ToolkitLockTypeInternal type) {
    ToolkitLockingApi.lock(lockId, type, platformService);
  }

  private void commitLock(Object lockId, final ToolkitLockTypeInternal type) {
    ToolkitLockingApi.unlock(lockId, type, platformService);
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(Object key) {
    final Object lockId = generateLockIdForKey(key);
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
      beginLock(getInstanceDsoLockName(), getEffectiveLockType());
      try {
        platformService.logicalInvoke(this, LogicalOperation.CLEAR_LOCAL_CACHE, NO_ARGS);
      } finally {
        commitLock(getInstanceDsoLockName(), getEffectiveLockType());
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
    return keySet(Collections.<K>emptySet());
  }

  @Override
  public Set<K> keySet(Set<K> filterSet) {
    Set keySet = null;
    try {
      keySet = tcObjectServerMap.keySet(this);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
    keySet.removeAll(filterSet);
    return new ServerMapKeySet<K, V>(this, keySet);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return entrySet(Collections.<K>emptySet());
  }

  @Override
  public Set<Entry<K, V>> entrySet(Set<K> filterSet) {

    Set keySet = null;
    try {
      keySet = tcObjectServerMap.keySet(this);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
    keySet.removeAll(filterSet);
    return new ServerMapEntrySet<K, V>(this, keySet);
  }

  private void assertNotNull(final Object value) {
    Preconditions.checkNotNull(value);
  }

  private void assertNull(final Object value) {
    if (null != value) { throw new AssertionError(); }
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
      // Do this outside the lock
      // NOTE - pass to extractor original value, not serialized version
      MetaData metaData = createMetaDataAndSetCommand(key, value, SearchCommand.PUT);
      eventualConcurrentLock.lock();
      try {
        V old = deserialize(key, asSerializedMapValue(doLogicalGetValueUnlocked(key)));
        doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
        return old;
      } finally {
        eventualConcurrentLock.unlock();
      }
    } else {
      MetaData metaData = createMetaDataAndSetCommand(key, value, SearchCommand.PUT);
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        doLogicalPutLocked(lockID, key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
        return old;
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }
  }

  @Override
  public void unlockedPutNoReturn(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    throttleIfNecessary();
    MetaData metaData = createMetaDataAndSetCommand(key, value, SearchCommand.PUT);
    doLogicalPutUnlocked(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
  }

  @Override
  public void unlockedPutNoReturnVersioned(K key, V value, long version, int createTimeInSecs, int customMaxTTISeconds,
                                           int customMaxTTLSeconds) {
    throttleIfNecessary();
    MetaData metaData = createMetaDataAndSetCommand(key, value, SearchCommand.PUT);
    doLogicalPut(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, MutateType.UNLOCKED,
                 null, metaData);
  }

  @Override
  public void putNoReturn(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    internalPutNoReturn(key, value, DEFAULT_VERSION, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void putVersioned(K key, V value, long version, int createTimeInSecs, int customMaxTTISeconds,
                           int customMaxTTLSeconds) {
    internalPutNoReturn(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  private void internalPutNoReturn(K key, V value, long version, int createTimeInSecs, int customMaxTTISeconds,
                                   int customMaxTTLSeconds) {
    assertNotNull(value);
    throttleIfNecessary();

    if (isEventual()) {
      MetaData metaData = createMetaDataAndSetCommand(key, value, SearchCommand.PUT);
      eventualConcurrentLock.lock();
      try {
        doLogicalPut(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds,
                     MutateType.UNLOCKED, null, metaData);
      } finally {
        eventualConcurrentLock.unlock();
      }
    } else {
      MetaData metaData = createMetaDataAndSetCommand(key, value, SearchCommand.PUT);
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        doLogicalPut(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds,
                     MutateType.LOCKED, lockID, metaData);
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }

  }

  @Override
  public void putIfAbsentVersioned(K key, V value, long version, int createTimeInSecs, int customMaxTTISeconds,
                                   int customMaxTTLSeconds) {
    assertNotNull(value);
    throttleIfNecessary();

    if (isEventual()) {
      eventualConcurrentLock.lock();
      try {
        unlockedPutIfAbsentNoReturnVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
      } finally {
        eventualConcurrentLock.unlock();
      }
    } else {
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        unlockedPutIfAbsentNoReturnVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }
  }

  @Override
  public void unlockedPutIfAbsentNoReturnVersioned(final K key, final V value, final long version, int createTimeInSecs,
                                                   int customMaxTTISeconds, int customMaxTTLSeconds) {
    final MetaData metaData = createMetaDataAndSetCommand(key, value, SearchCommand.PUT_IF_ABSENT);
    final K portableKey = assertKeyLiteral(key);
    final SerializedMapValue<V> serializedMapValue = createSerializedMapValue(value, createTimeInSecs,
                                                                           customMaxTTISeconds, customMaxTTLSeconds);

    unlockedPutIfAbsentNoReturnVersioned(portableKey, serializedMapValue, metaData, version);
  }

  private void unlockedPutIfAbsentNoReturnVersioned(final K key, final SerializedMapValue<V> serializedMapValue, final MetaData metaData, final long version) {
    this.tcObjectServerMap.doLogicalPutIfAbsentVersioned(key, serializedMapValue, version);
    if (metaData != null) {
      metaData.add(SearchMetaData.VALUE, serializedMapValue.getObjectID());
      addMetaData(metaData);
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
      K portableKey = (K) assertKeyLiteral(key);

      int retryCount = 1;
      while (true) {
        V valueObject = unsafeLocalGet(portableKey);
        if (valueObject != null) {
          return valueObject;
        }

        eventualConcurrentLock.lock();
        try {
          SerializedMapValue serializedMapValue = createSerializedMapValue(value, createTimeInSecs,
                                                                           customMaxTTISeconds, customMaxTTLSeconds);
          MetaData metaData = createPutSearchMetaData(portableKey, value);
          MetaDataDescriptor mdd = null;
          if (metaData != null) {
            metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT_IF_ABSENT);
            metaData.add(SearchMetaData.VALUE, serializedMapValue.getObjectID());
            mdd = getMetaDataDescriptor(metaData);
          }

          if (this.tcObjectServerMap.doLogicalPutIfAbsentUnlocked(this, portableKey, serializedMapValue, mdd)) {
            return null;
          } else {
            Object existingMapping = doLogicalGetValueUnlocked(portableKey);
            if (existingMapping != null) {
              return deserialize(key, asSerializedMapValue(existingMapping));
            }
          }
        } catch (AbortedOperationException e) {
          throw new ToolkitAbortableOperationException();
        } finally {
          eventualConcurrentLock.unlock();
        }

        if (retryCount % 10 == 0) {
          LOGGER.info("retried putIfAbsent for " + retryCount);
          retryCount++;
        }
      }
    } else {

      MetaData metaData = createPutSearchMetaData(key, value);
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
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
        commitLock(lockID, getEffectiveLockType());
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
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null) {
          doLogicalRemoveLocked(key, lockID);
        }
        return old;
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    return remove(key, value, DefaultToolkitValueComparator.INSTANCE);
  }

  @Override
  public boolean remove(final Object key, final Object value, ToolkitValueComparator comparator) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return false;
    }

    // TODO: what about value if its not Serializable

    assertNotNull(value);
    if (isEventual()) {
      SerializedMapValue oldSerializedMapValue = asSerializedMapValue(doLogicalGetValueUnlocked(key));
      V old = deserialize(key, oldSerializedMapValue);
      if (old != null && compare(old, (V) value, comparator)) {
        eventualConcurrentLock.lock();
        try {
          return doLogicalRemoveUnlocked(key, oldSerializedMapValue);
        } finally {
          eventualConcurrentLock.unlock();
        }
      } else {
        return false;
      }
    } else {
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null && compare(old, (V) value, comparator)) {
          doLogicalRemoveLocked(key, lockID);
          return true;
        } else {
          return false;
        }
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }
  }

  @Override
  public void unlockedRemoveNoReturn(final Object key) {
    doLogicalRemoveUnlocked(key);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    doLogicalRemoveUnlockedVersioned(key, version);
  }

  @Override
  public void removeNoReturn(final Object key) {
    internalRemoveNoReturn(key, DEFAULT_VERSION);
  }

  @Override
  public void removeNoReturnVersioned(final Object key, final long version) {
    internalRemoveNoReturn(key, version);
  }

  private void internalRemoveNoReturn(final Object key, final long version) {
    if (!LiteralValues.isLiteralInstance(key)) {
      // Returning null as we cannot key passed needs to be portable else if the key is not Literal
      return;
    }

    if (isEventual()) {
      eventualConcurrentLock.lock();
      try {
        internalLogicalRemove(key, version, MutateType.UNLOCKED, null);
      } finally {
        eventualConcurrentLock.unlock();
      }
    } else {
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        internalLogicalRemove(key, version, MutateType.LOCKED, lockID);
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }
  }

  @Override
  public V replace(final K key, final V value) {
    assertNotNull(value);

    throttleIfNecessary();
    MetaData metaData;
    if (isEventual()) {
      int retryCount = 0;
      while (true) {
        // retry until the old value found is correct, we can't send the old value from the backChannel for CAS as that
        // value will have been removed by inline DGC on L2 by now
        SerializedMapValue oldSerializedMapValue = asSerializedMapValue(doLogicalGetValueUnlocked(key));
        final V old = deserialize(key, oldSerializedMapValue);
        if (old == null) { return null; }
        metaData = createMetaDataAndSetCommand(key, value, SearchCommand.REPLACE);
        eventualConcurrentLock.lock();
        SerializedMapValue newSerializedMapValue = createSerializedMapValue(value, timeSource.nowInSeconds(),
                                                                            ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                                                                            ToolkitConfigFields.NO_MAX_TTL_SECONDS);
        try {
          MetaDataDescriptor mdd = null;
          if (metaData != null) {
            metaData.add(SearchMetaData.PREV_VALUE, oldSerializedMapValue.getObjectID());
            metaData.add(SearchMetaData.VALUE, newSerializedMapValue.getObjectID());
            mdd = getMetaDataDescriptor(metaData);
          }
          if (this.tcObjectServerMap.doLogicalReplaceUnlocked(this, key, oldSerializedMapValue, newSerializedMapValue,
                                                              mdd)) {
            return old;
          } else {
            // retry
          }

        } catch (AbortedOperationException e) {
          throw new ToolkitAbortableOperationException();
        } finally {
          eventualConcurrentLock.unlock();
        }
        if ((++retryCount % 10) == 0) {
          LOGGER.info("replace tried for " + key + " old " + oldSerializedMapValue.getObjectID() + " new "
                      + newSerializedMapValue.getObjectID() + " " + retryCount + " times.");
        }
      }
    } else {
      metaData = createPutSearchMetaData(key, value);
      if (metaData != null) {
        metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
      }

      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null) {
          doLogicalPut(key, value, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                       ToolkitConfigFields.NO_MAX_TTL_SECONDS, MutateType.LOCKED, lockID, metaData);
        }
        return old;
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    return replace(key, oldValue, newValue, DefaultToolkitValueComparator.INSTANCE);
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue, ToolkitValueComparator<V> comparator) {
    assertNotNull(oldValue);
    assertNotNull(newValue);
    throttleIfNecessary();

    MetaData metaData;
    if (isEventual()) {
      SerializedMapValue<V> oldSerializedMapValue = asSerializedMapValue(doLogicalGetValueUnlocked(key));
      final V old = deserialize(key, oldSerializedMapValue);
      if (old != null && compare(oldValue, old, comparator)) {
        metaData = createMetaDataAndSetCommand(key, newValue, SearchCommand.REPLACE);
        eventualConcurrentLock.lock();
        try {
          SerializedMapValue newSerializedMapValue = createSerializedMapValue(newValue, timeSource.nowInSeconds(),
                                                                              ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                                                                              ToolkitConfigFields.NO_MAX_TTL_SECONDS);
          MetaDataDescriptor mdd = null;
          if (metaData != null) {
            metaData.add(SearchMetaData.PREV_VALUE, oldSerializedMapValue.getObjectID());
            metaData.add(SearchMetaData.VALUE, newSerializedMapValue.getObjectID());
            mdd = getMetaDataDescriptor(metaData);
          }
          return this.tcObjectServerMap.doLogicalReplaceUnlocked(this, key, oldSerializedMapValue,
                                                                 newSerializedMapValue, mdd);

        } catch (AbortedOperationException e) {
          throw new ToolkitAbortableOperationException();
        } finally {
          eventualConcurrentLock.unlock();
        }
      } else {
        return false;
      }
    } else {
      metaData = createPutSearchMetaData(key, newValue);
      if (metaData != null) {
        metaData.set(SearchMetaData.COMMAND, SearchCommand.PUT);
      }

      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, getEffectiveLockType());
      try {
        final V old = deserialize(key, asSerializedMapValue(doLogicalGetValueLocked(key, lockID)));
        if (old != null && compare(oldValue, old, comparator)) {

          doLogicalPut(key, newValue, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                       ToolkitConfigFields.NO_MAX_TTL_SECONDS, MutateType.LOCKED, lockID, metaData);
          return true;
        } else {
          return false;
        }
      } finally {
        commitLock(lockID, getEffectiveLockType());
      }
    }
  }

  @Override
  public void unlockedClear() {
    tcObjectServerMap.doClear(this);
    updateSearchMetadataForClear();
  }

  private void updateSearchMetadataForClear() {
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
  public void clearVersioned() {
    beginLock(getInstanceDsoLockName(), this.lockType);
    try {
      tcObjectServerMap.doClearVersioned();
      updateSearchMetadataForClear();
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
  public VersionedValue<V> getVersionedValue(Object key) {
    GetType getType = isEventual() ? GetType.UNLOCKED : GetType.LOCKED;
    assertKeyLiteral(key);
    SerializedMapValue serializedMapValue = null;
    VersionedObject versionedObject = null;

    if (isEventual()) {
      versionedObject = doLogicalGetVersionedValue(key);
      serializedMapValue = asSerializedMapValue(versionedObject.getObject());
    } else {
      final Object lockID = generateLockIdForKey(key);
      beginLock(lockID, ToolkitLockTypeInternal.READ);
      try {
        versionedObject = doLogicalGetVersionedValue(key);
        serializedMapValue = asSerializedMapValue(versionedObject.getObject());
      } finally {
        commitLock(lockID, ToolkitLockTypeInternal.READ);
      }
    }

    V value = getNonExpiredValue(key, serializedMapValue, getType, true); // TODO: Revisit this method. It may be wrong
                                                                          // w.r.t WAN
    if (value != null) { return new VersionedValueImpl<V>(value, versionedObject.getVersion()); }

    return null;
  }

  @Override
  public Map<K, VersionedValue<V>> getAllVersioned(final SetMultimap<ObjectID, K> mapIdToKeysMap) {
    try {
      return Collections.unmodifiableMap(new HashMap<K, VersionedValue<V>>(
          Maps.transformEntries((Map)tcObjectServerMap.getAllVersioned((SetMultimap) mapIdToKeysMap),
              new Maps.EntryTransformer<Object, VersionedObject, VersionedValue<V>>() {
            @Override
            public VersionedValue<V> transformEntry(final Object key, final VersionedObject value) {
              if (value == null) {
                return null;
              }
              V nonExpiredValue = checkAndGetNonExpiredValue((K)key,
                  value.getObject(), GetType.UNLOCKED, true);
              if (nonExpiredValue == null) {
                return null;
              } else {
                return new VersionedValueImpl<V>(nonExpiredValue, value.getVersion());
              }
            }
          }
      )));
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
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

  void setSearchAttributeTypes(ToolkitMap<String, String> schema) {
    if (!this.attributeSchema.compareAndSet(null, schema) && schema != attributeSchema.get()) {
      LOGGER.warn(String.format("Ignoring attempt to reset search attribute schema on map %s", getName()));
    }
  }

  public void recalculateLocalCacheSize(Object key) {
    tcObjectServerMap.recalculateLocalCacheSize(assertKeyLiteral(key));
  }

  @Override
  public Collection<V> values() {
    return values(Collections.EMPTY_SET);
  }

  @Override
  public Collection<V> values(final Set<K> filterSet) {
    return new AbstractCollection<V>() {
      @Override
      public Iterator<V> iterator() {
        return new Iterator<V>() {
          private final Iterator<Entry<K, V>> i = entrySet(filterSet).iterator();

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

  @Override
  public void cleanupOnDestroy() {
    disposeLocally();
  }

  @Override
  public void addSelfToTxn() {
    tcObjectServerMap.logicalInvoke(LogicalOperation.NO_OP, new Object[0]);
  }

  @Override
  public void takeSnapshot(SearchRequestID queryId) {
    addSnapshotMetaData(queryId, false);
    tcObjectServerMap.logicalInvoke(LogicalOperation.NO_OP, new Object[0]);
  }

  @Override
  public void releaseSnapshot(SearchRequestID queryId) {
    beginLock(getInstanceDsoLockName(), this.lockType);
    try {
      tcObjectServerMap.logicalInvoke(LogicalOperation.NO_OP, new Object[0]);
      addSnapshotMetaData(queryId, true);
    } finally {
      commitLock(getInstanceDsoLockName(), this.lockType);
    }

  }
  @Override
  protected void doLogicalDestroy() {
    super.doLogicalDestroy();
    MetaData destroyMetadata = createBaseMetaData();
    destroyMetadata.set(SearchMetaData.COMMAND, SearchCommand.DESTROY);
    addMetaData(destroyMetadata);
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
      platformService.logicalInvoke(this, LogicalOperation.INT_FIELD_CHANGED, new Object[] {
          ServerMapApplicator.MAX_TTI_SECONDS_FIELDNAME, this.maxTTISeconds });
    } finally {
      internalClearLocalCache();
    }
  }

  private void setMaxTTL(int intValue) {
    try {
      this.maxTTLSeconds = intValue;
      platformService.logicalInvoke(this, LogicalOperation.INT_FIELD_CHANGED, new Object[] {
          ServerMapApplicator.MAX_TTL_SECONDS_FIELDNAME, this.maxTTLSeconds });
    } finally {
      internalClearLocalCache();
    }
  }

  private void setMaxTotalCount(int intValue) {
    try {
      this.maxCountInCluster = intValue;
      platformService.logicalInvoke(this, LogicalOperation.INT_FIELD_CHANGED, new Object[] {
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
      platformService.logicalInvoke(this, LogicalOperation.FIELD_CHANGED, new Object[] {
          ServerMapApplicator.EVICTION_ENABLED_FIELDNAME, this.evictionEnabled });
    }
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

  private void addSnapshotMetaData(SearchRequestID queryId, boolean isClose) {
    if (!isSearchable()) throw new UnsupportedOperationException("search is not enabled");
    MetaData snapMetadata = createBaseMetaData();
    snapMetadata.set(SearchMetaData.COMMAND, isClose ? SearchCommand.RELEASE_RESULTS : SearchCommand.SNAPSHOT);
    snapMetadata.add(SearchMetaData.CLIENT_ID, platformService.getClientId());
    snapMetadata.add(SearchMetaData.REQUEST_ID, queryId.toLong());
    addMetaData(snapMetadata);

  }

  private static String getSearchAttributeType(String name, Object value) {
    ToolkitAttributeType type = ToolkitAttributeType.typeFor(name, value);
    // Correctly handle distinct types of enums
    return ToolkitAttributeType.ENUM == type ? ((Enum) value).getDeclaringClass().getName() : type.name();
  }

  private MetaData createMetaDataAndSetCommand(K key, V value, SearchCommand command) {
    MetaData metaData = createPutSearchMetaData(key, value);
    if (metaData != null) {
      metaData.set(SearchMetaData.COMMAND, command);
    }
    return metaData;
  }

  private MetaData createPutSearchMetaData(K key, V value) {
    if (!isSearchable()) return null;

    MetaData md = createBaseMetaData();
    md.add(SearchMetaData.KEY, key);

    try {
      Map<String, String> recordedTypes = new HashMap<String, String>();
      Map<String, String> searchAttributeTypes = attributeSchema.get();
      boolean updateTypes = searchAttributeTypes.isEmpty();

      Map<String, Object> attrs = attrExtractor.attributesFor(key, value);
      if (attrs == ToolkitAttributeExtractor.DO_NOT_INDEX) return null;
      for (Map.Entry<String, Object> attr : attrs.entrySet()) {
        String attrName = attr.getKey();
        Object attrValue = attr.getValue();

        String type;
        if (attrValue != null) {
          if (updateTypes) {
            type = getSearchAttributeType(attrName, attrValue);
            recordedTypes.put(attrName, type);
          } else {
            type = searchAttributeTypes.get(attrName);
            String resolvedType = getSearchAttributeType(attrName, attrValue);
            if (type != null) {
              if (!type.equals(resolvedType)) { throw new SearchException(
                                                                          String
                                                                              .format("Expecting a %s value for attribute [%s] but was %s",
                                                                                      type, attrName, resolvedType)); }
            } else {
              recordedTypes.put(attrName, resolvedType);
            }
          }

          md.add(SearchMetaData.ATTR + attr.getKey(), attr.getValue());
        }
      }

      if (!recordedTypes.isEmpty()) {
        // check that we're overwriting any existing entries
        String lockId = getInstanceDsoLockName() + getAttrTypeMapLockName();
        beginLock(lockId, ToolkitLockTypeInternal.WRITE);
        try {
          for (Entry<String, String> e : recordedTypes.entrySet()) {
            String attrName = e.getKey();
            String existing = searchAttributeTypes.get(attrName);
            String newType = e.getValue();
            if (existing != null && !existing.equals(newType)) {
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
      if (this.l1ServerMapLocalCacheStore != null) {
        this.l1ServerMapLocalCacheStore.dispose();
        this.l1ServerMapLocalCacheStore = null;
      }
    }
  }

  @Override
  public void registerAttributeExtractor(ToolkitAttributeExtractor extractor) {
    this.attrExtractor = extractor;
  }

  private class LongLockStrategy implements LockStrategy {

    private final long highBits;
    public LongLockStrategy(String instanceQualifier) {
      this.highBits = ((long) instanceQualifier.hashCode()) << 32;
    }

    @Override
    public Object generateLockIdForKey(Object key) {
      long lowBits = computeHashCode(key) & 0x00000000FFFFFFFFL;
      return highBits | lowBits;
    }

    protected int computeHashCode(Object key) {
      byte[] bytes = null;
      int hash = 1704124966;
      try {
        bytes = ((String) key).getBytes("UTF-8");
      } catch (Exception e) {
        return key.hashCode();
      }
      for (byte b : bytes) {
        hash ^= b;
        hash *= 0x01000193;
      }
      return hash;
    }

  }

  private class Operation<T> implements BufferedOperation<T> {
    private final Type type;
    private final T    value;
    private final long version;
    private final SerializedMapValueParameters<T> smvParams;

    Operation(Type type, T value, long version, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
      this.type = type;
      this.value = value;
      this.version = version;
      smvParams = type != Type.REMOVE ? createSerializedMapValueParameters(value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds) : null;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public boolean isVersioned() {
      return this.version != LocalBufferedMap.NO_VERSION;
    }

    @Override
    public int getCreateTimeInSecs() {
      return smvParams.getCreateTime();
    }

    @Override
    public int getCustomMaxTTISeconds() {
      return smvParams.getCustomTTI();
    }

    @Override
    public int getCustomMaxTTLSeconds() {
      return smvParams.getCustomTTL();
    }

    @Override
    public long getVersion() {
      // Not equivalent to smv.getVersion()! that's the object version, this one is the mapping version.
      return version;
    }

    SerializedMapValueParameters<T> getSerializedMapValueParams() {
      return smvParams;
    }
  }

  private class StringLockStrategy implements LockStrategy {

    public StringLockStrategy() {
      // empty ctor
    }

    @Override
    public Object generateLockIdForKey(Object key) {
      return KEY_LOCK_PREFIX + name + key;
    }
  }

  private class ObjectLockStrategy implements LockStrategy {

    public ObjectLockStrategy() {
      // empty ctor
    }

    @Override
    public Object generateLockIdForKey(Object key) {
      return LOCK_PREFIX + name;
    }
  }

  @Override
  public void addTxnInProgressKeys(Set<K> addSet, Set<K> removeSet) {
    tcObjectServerMap.addTxnInProgressKeys(addSet, removeSet);
  }

  private boolean compare(V v1, V v2, ToolkitValueComparator<V> comparator) {
    return comparator.equals(v1, v2);
  }

  @Override
  public void registerListener(Set<ServerEventType> eventTypes, boolean skipRejoinChecks) {
    assertNotNull(eventTypes);
    eventualConcurrentLock.lock();
    try {
      tcObjectServerMap.doRegisterListener(eventTypes, skipRejoinChecks);
    } finally {
      eventualConcurrentLock.unlock();
    }
  }

  @Override
  public void unregisterListener(Set<ServerEventType> eventTypes) {
    assertNotNull(eventTypes);
    eventualConcurrentLock.lock();
    try {
      tcObjectServerMap.doUnregisterListener(eventTypes);
    } finally {
      eventualConcurrentLock.unlock();
    }
  }

  @Override
  public void drain(final Map<K, BufferedOperation<V>> buffer) {
    throttleIfNecessary();
    eventualConcurrentLock.lock();
    try {
      // for single serverMap
      for (Entry<K, BufferedOperation<V>> e : buffer.entrySet()) {
        BufferedOperation<V> operation = e.getValue();
        MetaData metaData;
        SerializedMapValue<V> smv = createSerializedMapValue(operation);
        long version = operation.isVersioned() ? operation.getVersion() : DEFAULT_VERSION;
        switch (operation.getType()) {
          case PUT:
            metaData = createMetaDataAndSetCommand(e.getKey(), operation.getValue(), SearchCommand.PUT);
            doLogicalPut(version, MutateType.UNLOCKED, null, metaData, e.getKey(), smv);
            break;
          case PUT_IF_ABSENT:
            if (!operation.isVersioned()) {
              // putIfAbsent returns by default, so a buffered up putIfAbsent doesn't really work...
              throw new UnsupportedOperationException("Can't do buffered putIfAbsent");
            }
            // "versioned" variant of putIfAbsent does not return
            metaData = createMetaDataAndSetCommand(e.getKey(), operation.getValue(), SearchCommand.PUT_IF_ABSENT);
            unlockedPutIfAbsentNoReturnVersioned(e.getKey(), smv, metaData, version);
            break;
          case REMOVE:
            internalLogicalRemove(e.getKey(), version, MutateType.UNLOCKED, null);
            break;
        }
      }
    } finally {
      eventualConcurrentLock.unlock();
    }
  }

  private <T> SerializedMapValue<T> createSerializedMapValue(BufferedOperation<T> bufferedOperation) {
    if (bufferedOperation.getType() == BufferedOperation.Type.REMOVE) {
      return null;
    }
    if (bufferedOperation instanceof Operation) {
      return serializedClusterObjectFactory.createSerializedMapValue(
          ((Operation<T>)bufferedOperation).getSerializedMapValueParams(), gid);
    } else {
      SerializedMapValueParameters<T> parameters = createSerializedMapValueParameters(bufferedOperation.getValue(),
          bufferedOperation.getCreateTimeInSecs(), bufferedOperation.getCustomMaxTTISeconds(),
          bufferedOperation.getCustomMaxTTLSeconds());
      return serializedClusterObjectFactory.createSerializedMapValue(parameters, gid);
    }
  }

  @Override
  public BufferedOperation<V> createBufferedOperation(final BufferedOperation.Type type, final K key, final V value,
                                                      final long version, final int createTimeInSecs, final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    return new Operation<V>(type, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }
}
