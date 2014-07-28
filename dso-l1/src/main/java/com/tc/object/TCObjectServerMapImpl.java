/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import static com.tc.server.VersionedServerEvent.DEFAULT_VERSION;

import com.google.common.base.Preconditions;
import com.google.common.collect.SetMultimap;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNA;
import com.tc.object.locks.LockID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.servermap.ExpirableMapEntry;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TCObjectServerMapImpl<L> extends TCObjectLogical implements TCObjectServerMap<L> {

  private final static TCLogger               logger                          = TCLogging
                                                                                  .getLogger(TCObjectServerMapImpl.class);

  private static final Object[]               NO_ARGS                         = {};

  private static final long                   GET_VALUE_FOR_KEY_LOG_THRESHOLD = 10 * 1000;

  // The condition that can require a retry is transient so we could retry immediately, this throttle is here just to
  // prevent hammering the server
  private static final long                   RETRY_GET_VALUE_FOR_KEY_SLEEP   = 10;

  private final Lock[]                        localLocks;

  static {
    boolean deprecatedProperty = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED);
    if (!deprecatedProperty) {
      // trying to disable is not supported
      logger.warn("The property '" + TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED
                  + "' has been deprecated, set the localCacheEnabled to false in config to disable local caching.");
    }
  }

  private final GroupID                       groupID;
  private final ClientObjectManager           objectManager;
  private final RemoteServerMapManager        serverMapManager;
  private final Manager                       manager;
  private volatile ServerMapLocalCache        cache;
  private volatile boolean                    isEventual;
  private volatile boolean                    localCacheEnabled;

  private volatile L1ServerMapLocalCacheStore serverMapLocalStore;
  private final TCObjectSelfStore             tcObjectSelfStore;
  final L1ServerMapLocalCacheManager          globalLocalCacheManager;
  private volatile PinnedEntryFaultCallback   callback;
  private volatile boolean                    createdOnServer;

  public TCObjectServerMapImpl(final Manager manager, final ClientObjectManager objectManager,
                               final RemoteServerMapManager serverMapManager, final ObjectID id, final Object peer,
                               final TCClass tcc, final boolean isNew,
                               final L1ServerMapLocalCacheManager globalLocalCacheManager) {
    super(id, peer, tcc, isNew);
    this.tcObjectSelfStore = globalLocalCacheManager;
    this.groupID = new GroupID(id.getGroupID());
    this.objectManager = objectManager;
    this.serverMapManager = serverMapManager;
    this.manager = manager;
    this.globalLocalCacheManager = globalLocalCacheManager;
    if (serverMapLocalStore != null) {
      setupLocalCache(serverMapLocalStore, callback);
    }
    int concurrency = TCPropertiesImpl.getProperties().getInt("tcObjectServerMapConcurrency", 8);
    localLocks = new Lock[concurrency];
    for (int i = 0; i < concurrency; i++) {
      localLocks[i] = new ReentrantLock();
    }
    if (isNew) {
      manager.addTransactionCompleteListener(new TransactionCompleteListener() {

        @Override
        public void transactionComplete(TransactionID txnID) {
          createdOnServer = true;
        }

        @Override
        public void transactionAborted(TransactionID txnID) {
          // TODO: handle this with atomic transaction rollback.
        }
      });
    } else {
      createdOnServer = true;
    }
  }

  @Override
  public void initialize(final int maxTTISeconds, final int maxTTLSeconds, final int targetMaxTotalCount,
                         final boolean isCacheEventual, final boolean localCacheEnabledFlag) {
    this.isEventual = isCacheEventual;
    this.localCacheEnabled = localCacheEnabledFlag;
    // if tcobject is being faulted in, the TCO is created and the peer is hydrated afterwards
    // meaning initialize is called after the cache has been already created, need to update
    if (cache != null) {
      cache.setLocalCacheEnabled(localCacheEnabledFlag);
    }
  }

  /**
   * Does a logical put and updates the local cache
   * 
   * @param lockID Lock under which this change is made
   * @param key Key Object
   * @param value Object in the mapping
   */
  @Override
  public void doLogicalPut(final L lockID, final Object key, final Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      ObjectID valueObjectID = invokeLogicalPut(key, value);
      addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value, valueObjectID,
                            MapOperationType.PUT);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a logical put with version and updates the local cache.
   * 
   * @param map ServerTCMap
   * @param lockID Lock under which this change is made
   * @param key Key Object
   * @param value Object in the mapping
   * @param version
   */
  @Override
  public void doLogicalPutVersioned(final TCServerMap map, final L lockID, final Object key, final Object value,
                                    final long version) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      final ObjectID valueObjectID = invokeLogicalPutVersioned(key, value, version);
      addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value, valueObjectID,
          MapOperationType.PUT);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void doClear(TCServerMap map) {
    logicalInvoke(LogicalOperation.CLEAR, NO_ARGS);
  }

  @Override
  public void doClearVersioned() {
    logicalInvoke(LogicalOperation.CLEAR_VERSIONED, NO_ARGS);
  }

  /**
   * Does a logical put and updates the local cache but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  @Override
  public void doLogicalPutUnlocked(TCServerMap map, Object key, Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      ObjectID valueObjectID = invokeLogicalPut(key, value);

      updateLocalCacheOnPut(key, value, valueObjectID);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a logical put and updates the local cache but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   * @param version
   */
  @Override
  public void doLogicalPutUnlockedVersioned(final TCServerMap map, final Object key, final Object value,
                                            final long version) {
    final Lock lock = getLockForKey(key);
    lock.lock();
    try {
      final ObjectID valueObjectID = invokeLogicalPutVersioned(key, value, version);
      updateLocalCacheOnPut(key, value, valueObjectID);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void doLogicalPutIfAbsentVersioned(final Object key, final Object value, final long version) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      final ObjectID valueObjectID = invokeLogicalPutIfAbsentVersioned(key, value, version);
      updateLocalCacheOnPut(key, value, valueObjectID);
    } finally {
      lock.unlock();
    }
  }

  private void updateLocalCacheOnPut(final Object key, final Object value, final ObjectID valueObjectID) {
    if (!isEventual) {
      addIncoherentValueToCache(key, value, valueObjectID, MapOperationType.PUT);
    } else {
      addEventualValueToCache(key, value, valueObjectID, MapOperationType.PUT);
    }
  }

  private void removeIfTCObjectSelf(Object value) {
    if (value instanceof TCObjectSelf) {
      this.tcObjectSelfStore.removeTCObjectSelfTemp((TCObjectSelf) value, true);
    }
  }

  @Override
  public boolean doLogicalPutIfAbsentUnlocked(TCServerMap map, Object key, Object value, MetaDataDescriptor mdd)
      throws AbortedOperationException {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      shareObject(key);
      final ObjectID valueObjectID = shareObject(value);
      final Object[] parameters = constructParams(key, value);
      if (mdd != null) {
        addMetaData(mdd);
      }
      boolean rv;
      try {
        rv = logicalInvokeWithResult(LogicalOperation.PUT_IF_ABSENT, parameters);
      } catch (AbortedOperationException e) {
        // Timed out. Don't know if the logical invoke succeeded so just assume it failed and dump the value, we can
        // refetch it afterwards.
        removeIfTCObjectSelf(value);
        throw e;
      }
      if (rv) {
        updateLocalCacheOnPut(key, value, valueObjectID);
      } else {
        removeIfTCObjectSelf(value);
      }
      return rv;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object current, Object newValue,
                                          MetaDataDescriptor mdd) throws AbortedOperationException {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      shareObject(key);
      shareObject(current);
      final ObjectID valueObjectID = shareObject(newValue);
      final Object[] parameters = constructParamsForReplace(key, newValue, current);
      if (mdd != null) {
        addMetaData(mdd);
      }
      boolean rv;
      try {
        rv = logicalInvokeWithResult(LogicalOperation.REPLACE_IF_VALUE_EQUAL, parameters);
      } catch (AbortedOperationException e) {
        // Don't know what the result is since we timed out, so just dump both the new value and the old value and
        // refetch from the server.
        removeValueFromLocalCache(key);
        removeIfTCObjectSelf(newValue);
        throw e;
      }
      if (rv) {
        updateLocalCacheOnPut(key, newValue, valueObjectID);
      } else {
        removeIfTCObjectSelf(newValue);
      }
      return rv;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a logic expire and removes from the local cache if present
   * 
   * @param lockID, lock under which this entry is expired
   * @param key key object
   * @param value value object
   */
  @Override
  public void doLogicalExpire(final L lockID, final Object key, final Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      invokeLogicalExpire(key, value);
      updateCacheOnRemove(lockID, key);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map TCServerMap
   * @param lockID, lock under which this entry is removed
   * @param key Key Object
   */
  @Override
  public void doLogicalRemove(final TCServerMap map, final L lockID, final Object key) {
    final Lock lock = getLockForKey(key);
    lock.lock();
    try {
      invokeLogicalRemove(key);
      updateCacheOnRemove(lockID, key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void doLogicalRemoveVersioned(final TCServerMap map, final L lockID, final Object key, final long version) {
    final Lock lock = getLockForKey(key);
    lock.lock();
    try {
      invokeLogicalRemoveVersioned(key, version);
      updateCacheOnRemove(lockID, key);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   */
  @Override
  public void doLogicalRemoveUnlocked(final TCServerMap map, final Object key) {
    final Lock lock = getLockForKey(key);
    lock.lock();
    try {
      invokeLogicalRemove(key);
      updateCacheOnRemoveUnlocked(key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void doLogicalRemoveUnlockedVersioned(final TCServerMap map, final Object key, final long version) {
    final Lock lock = getLockForKey(key);
    lock.lock();
    try {
      invokeLogicalRemoveVersioned(key, version);
      updateCacheOnRemoveUnlocked(key);
    } finally {
      lock.unlock();
    }
  }

  private void updateCacheOnRemove(final L lockID, final Object key) {
    final LockID id = this.manager.generateLockIdentifier(lockID);
    addStrongValueToCache(id, key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
  }

  private void updateCacheOnRemoveUnlocked(final Object key) {
    if (!isEventual) {
      addIncoherentValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
    } else {
      addEventualValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
    }
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @return remove success
   * @throws AbortedOperationException
   */
  @Override
  public boolean doLogicalRemoveUnlocked(TCServerMap map, Object key, Object value, MetaDataDescriptor mdd)
      throws AbortedOperationException {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
      if (item != null && value != item.getValueObject()) {
        // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
        // and coz of DEV-5462
        return false;
      }
      // Only add metadata if we are sending the change..
      if (mdd != null) {
        addMetaData(mdd);
      }
      // Just remove the value ahead of time, worst case we go out to the server and pick it up again if the remove fails
      removeValueFromLocalCache(key);
      return invokeLogicalRemove(key, value);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a two arg logical expire and removes from the local cache if present but without an associated lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @return {@code true} if value was mutated, otherwise {@code false}
   */
  @Override
  public boolean doLogicalExpireUnlocked(final TCServerMap map, final Object key, final Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      final AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
      if (item != null && value != item.getValueObject()) {
        // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
        // and coz of DEV-5462
        return false;
      }

      invokeLogicalExpire(key, value);
      updateCacheOnRemoveUnlocked(key);
      return true;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @param lockID Lock under which this call is made
   * @return value Object in the mapping, null if no mapping present.
   * @throws AbortedOperationException
   */
  @Override
  public Object getValue(final TCServerMap map, final L lockID, final Object key) throws AbortedOperationException {
    if (!isCacheInitialized()) { return null; }
    AbstractLocalCacheStoreValue item = this.cache.getLocalValueStrong(key);
    if (item != null) { return item.getValueObject(); }
    // Doing double checking to ensure correct value
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      item = this.cache.getLocalValueStrong(key);
      if (item != null) { return item.getValueObject(); }

      Object value = getValueForKeyFromServer(map, key, false, false);

      if (value != null) {
        addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value,
                              objectManager.lookupExistingObjectID(value), MapOperationType.GET);
      }
      return value;
    } finally {
      lock.unlock();
    }
  }

  private void updateLocalCacheIfNecessary(final Object key, final Object value) {
    // Null values (i.e. cache misses) & literal values are not cached locally
    if (value != null && !LiteralValues.isLiteralInstance(value)) {
      if (isEventual) {
        //
        addEventualValueToCache(key, value, this.objectManager.lookupExistingObjectID(value), MapOperationType.GET);
      } else {
        addIncoherentValueToCache(key, value, this.objectManager.lookupExistingObjectID(value), MapOperationType.GET);
      }
    }
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap outside a lock context.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   * @throws AbortedOperationException
   */
  @Override
  public Object getValueUnlocked(TCServerMap map, Object key) throws AbortedOperationException {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null) { return item.getValueObject(); }

    // Doing double checking to ensure correct value
    final Lock lock = getLockForKey(key);
    lock.lock();
    try {
      item = getValueUnlockedFromCache(key);
      if (item != null) { return item.getValueObject(); }

      final Object value = getValueForKeyFromServer(map, key, true, false);
      if (value != null) {
        updateLocalCacheIfNecessary(key, value);
      }
      return value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public VersionedObject getVersionedValue(final TCServerMap map, final Object key) throws AbortedOperationException {
    if (!isCacheInitialized()) { return null; }

    final Lock lock = getLockForKey(key);
    lock.lock();
    try {
      final VersionedObject value = (VersionedObject) getValueForKeyFromServer(map, key, true, true);
      if (value != null) {
        updateLocalCacheIfNecessary(key, value.getObject());
      }
      return value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Map<Object, Object> getAllValuesUnlocked(final SetMultimap<ObjectID, Object> mapIdToKeysMap)
      throws AbortedOperationException {
    Map<Object, Object> rv = new HashMap<Object, Object>();
    for (Iterator<Entry<ObjectID, Object>> i = mapIdToKeysMap.entries().iterator(); i.hasNext();) {
      Object key = i.next().getValue();
      AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
      if (item != null) {
        rv.put(key, item.getValueObject());
        i.remove();
      }
    }

    // if everything was in local cache
    if (mapIdToKeysMap.isEmpty()) return rv;
    if (!createdOnServer) {
      // add null for the values as no data is present on server.
      for (Entry<ObjectID, Object> entry : mapIdToKeysMap.entries()) {
        rv.put(entry.getValue(), null);
      }
    } else {
      getAllValuesForKeyFromServer(mapIdToKeysMap, rv, false);
    }
    return rv;
  }

  @Override
  public Map<Object, VersionedObject> getAllVersioned(final SetMultimap<ObjectID, Object> mapIdToKeysMap) throws AbortedOperationException {
    Map<Object, Object> rv = new HashMap<Object, Object>();
    getAllValuesForKeyFromServer(mapIdToKeysMap, rv, true);
    return (Map) rv;
  }

  private AbstractLocalCacheStoreValue getValueUnlockedFromCache(Object key) {
    if (!isCacheInitialized()) { return null; }
    return this.cache.getLocalValue(key);
  }

  public void addStrongValueToCache(LockID lockId, Object key, Object value, ObjectID valueObjectID,
                                    MapOperationType mapOperation) {
    final LocalCacheStoreStrongValue localCacheValue = new LocalCacheStoreStrongValue(lockId, value, valueObjectID,
                                                                                      manager.getLockAwardIDFor(lockId));
    addToCache(key, localCacheValue, valueObjectID, mapOperation);
  }

  public void addEventualValueToCache(Object key, Object value, ObjectID valueObjectID, MapOperationType mapOperation) {
    final LocalCacheStoreEventualValue localCacheValue = new LocalCacheStoreEventualValue(valueObjectID, value);
    addToCache(key, localCacheValue, valueObjectID, mapOperation);
  }

  public void addIncoherentValueToCache(Object key, Object value, ObjectID valueObjectID, MapOperationType mapOperation) {
    final LocalCacheStoreEventualValue localCacheValue = new LocalCacheStoreEventualValue(valueObjectID, value);
    addToCache(key, localCacheValue, valueObjectID, mapOperation);
  }

  private void addToCache(Object key, final AbstractLocalCacheStoreValue localCacheValue, ObjectID valueOid,
                          MapOperationType mapOperation) {
    Object value = localCacheValue.getValueObject();
    boolean notifyServerForRemove = false;
    if (value instanceof TCObjectSelf) {
      if (localCacheEnabled || mapOperation.isMutateOperation()) {
        if (!this.tcObjectSelfStore.addTCObjectSelf(serverMapLocalStore, localCacheValue, value,
                                                    mapOperation.isMutateOperation())) { return; }
      } else {
        notifyServerForRemove = true;
      }
    }

    if (isCacheInitialized() && (localCacheEnabled || mapOperation.isMutateOperation())) {
      cache.addToCache(key, localCacheValue, mapOperation);
    }

    if ((value instanceof TCObjectSelf) && notifyServerForRemove) {
      this.tcObjectSelfStore.removeTCObjectSelfTemp((TCObjectSelf) value, notifyServerForRemove);
    }
  }

  private Object getValueForKeyFromServer(final TCServerMap map, final Object key, final boolean retry,
                                          final boolean versionRequired) throws AbortedOperationException {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getValueForKeyInMap is not supported in a non-shared ServerMap"); }
    if (!createdOnServer) { return null; }

    final ObjectID mapID = tcObject.getObjectID();
    Object portableKey = getPortableKey(key);

    // If the key->value mapping is changed in some way during this lookup, it's possible that the value object
    // originally pointed to was deleted by DGC. If that happens we retry until we get a good value.
    long start = System.nanoTime();
    while (true) {
      final CompoundResponse value = (CompoundResponse) this.serverMapManager.getMappingForKey(mapID, portableKey);
      try {
        Object object = lookupValue(value);
        if (versionRequired) {
          object = new VersionedObject(object, value.getVersion());
        }
        return object;
      } catch (TCObjectNotFoundException e) {
        if (!retry) {
          logger.warn("TCObjectNotFoundException for object " + value + " on a locked get. Returning null.");
          return null;
        }
        long timeWaited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (timeWaited > GET_VALUE_FOR_KEY_LOG_THRESHOLD) {
          logger.warn("Value for key: " + key + " still not found after " + timeWaited + "ms.");
          start = System.nanoTime();
        }
        ThreadUtil.reallySleep(RETRY_GET_VALUE_FOR_KEY_SLEEP);
      }
    }
  }

  private Object getPortableKey(final Object key) {
    Object portableKey = key;
    if (key instanceof Manageable) {
      final TCObject keyObject = ((Manageable) key).__tc_managed();
      if (keyObject == null) { throw new UnsupportedOperationException(
                                                                       "Key is portable, but not shared. This is currently not supported with TCObjectServerMap. Key = "
                                                                           + key); }
      portableKey = keyObject.getObjectID();
    }

    if (!LiteralValues.isLiteralInstance(portableKey)) {
      // formatter
      throw new UnsupportedOperationException(
                                              "Key is not portable. It needs to be a liternal or portable and shared for TCObjectServerMap. Key = "
                                                  + portableKey);
    }
    return portableKey;
  }

  private Object lookupValue(final CompoundResponse value) throws TCObjectNotFoundException, AbortedOperationException {
    try {
      final ObjectID oid = (value.getData() instanceof ObjectID) ? (ObjectID) value.getData() : ((DNA) value.getData())
          .getObjectID();

      if (oid.isNull()) { return null; }

      final Object returnValue = this.objectManager.lookupObjectQuiet(oid);
      if (returnValue instanceof ExpirableMapEntry) {
        ExpirableMapEntry expirableMapEntry = (ExpirableMapEntry) returnValue;
        expirableMapEntry.setCreationTime(value.getCreationTime());
        expirableMapEntry.setLastAccessedTime(value.getLastAccessedTime());
        expirableMapEntry.setTimeToIdle(value.getTimeToIdle());
        expirableMapEntry.setTimeToLive(value.getTimeToLive());
      }

      return returnValue;
    } catch (final ClassNotFoundException e) {
      logger.warn("Got ClassNotFoundException for objectId: " + value + ". Ignoring exception and returning null");
      return null;
    }
  }

  private TCObjectServerMapImpl lookupTCObjectServerMapImpl(final ObjectID mapID) throws AbortedOperationException {
    try {
      return (TCObjectServerMapImpl) this.objectManager.lookup(mapID);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("ClassNotFoundException for mapID " + mapID);
    }
  }

  private <K> Set<Object> getAllPortableKeys(final Set<K> keys) {
    Set<Object> portableKeys = new HashSet<Object>();
    for (K key : keys) {
      portableKeys.add(getPortableKey(key));
    }
    return portableKeys;
  }

  private void getAllValuesForKeyFromServer(final SetMultimap<ObjectID, Object> mapIdToKeysMap, Map<Object, Object> rv, boolean versioned)
      throws AbortedOperationException {
    if (!createdOnServer) {
      // add null for the values as no data is present on server.
      for (Entry<ObjectID, Object> objectIDObjectEntry : mapIdToKeysMap.entries()) {
        rv.put(objectIDObjectEntry.getValue(), null);
      }
      return;
    }
    final Map<ObjectID, Set<Object>> mapIdsToLookup = new HashMap<ObjectID, Set<Object>>();
    for (Entry<ObjectID, Collection<Object>> entry : mapIdToKeysMap.asMap().entrySet()) {
      // converting Map from <mapID, key> to <mapID, portableKey>
      mapIdsToLookup.put(entry.getKey(), getAllPortableKeys((Set<Object>) entry.getValue()));
    }

    long start = System.nanoTime();
    while (!mapIdsToLookup.isEmpty()) {
      this.serverMapManager.getMappingForAllKeys(mapIdsToLookup, rv);

      for (Iterator<Entry<ObjectID, Set<Object>>> lookupIterator = mapIdsToLookup.entrySet().iterator(); lookupIterator
          .hasNext();) {
        Entry<ObjectID, Set<Object>> entry = lookupIterator.next();
        TCObjectServerMapImpl map = lookupTCObjectServerMapImpl(entry.getKey());
        Set<Object> portableKeys = entry.getValue();
        for (Iterator<Object> portableKeyIterator = portableKeys.iterator(); portableKeyIterator.hasNext();) {
          Object key = portableKeyIterator.next();
          CompoundResponse value = (CompoundResponse) rv.get(key);
          Object data;
          try {
            data = lookupValue(value);
          } catch (TCObjectNotFoundException e) {
            // We weren't able to find this particular mapping, continue for now, and try again on another pass
            continue;
          }
          portableKeyIterator.remove();

          // update the local cache of corresponding TCServerMap
          map.updateLocalCacheIfNecessary(key, data);
          if (versioned) {
            rv.put(key, data == null ? null : new VersionedObject(data, value.getVersion()));
          } else {
            rv.put(key, data);
          }
        }

        // Remove the map from the remaining lookups when all its keys are accounted for.
        if (portableKeys.isEmpty()) {
          lookupIterator.remove();
        }
      }
      // Check if we have more lookups to do
      if (!mapIdsToLookup.isEmpty()) {
        long timeWaited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (timeWaited > GET_VALUE_FOR_KEY_LOG_THRESHOLD) {
          logger.warn("Still waiting for values after " + timeWaited + "ms.");
          start = System.nanoTime();
        }
        ThreadUtil.reallySleep(RETRY_GET_VALUE_FOR_KEY_SLEEP);
      }
    }
  }

  /**
   * Returns a snapshot of keys for the giver ServerTCMap
   * 
   * @param map ServerTCMap
   * @return set Set return snapshot of keys
   */
  @Override
  public Set keySet(final TCServerMap map) throws AbortedOperationException {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException("keySet is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    if (!createdOnServer) {
      return Collections.emptySet();
    } else {
      return this.serverMapManager.getAllKeys(mapID);
    }
  }

  /**
   * Returns total size of an array of ServerTCMap.
   * <P>
   * The list of TCServerMaps passed in need not contain this TCServerMap, this is only a pass thru method that calls
   * getAllSize on the RemoteServerMapManager and is provided as a convenient way of batching the size calls at the
   * higher level.
   * 
   * @param maps ServerTCMap[]
   * @return long for size of map.
   * @throws AbortedOperationException
   */
  @Override
  public long getAllSize(final TCServerMap[] maps) throws AbortedOperationException {
    final ObjectID[] mapIDs = new ObjectID[maps.length];
    for (int i = 0; i < maps.length; ++i) {
      TCServerMap map = maps[i];
      final TCObject tcObject = map.__tc_managed();
      if (tcObject == null) { throw new UnsupportedOperationException(
                                                                      "getSize is not supported in a non-shared ServerMap"); }
      mapIDs[i] = tcObject.getObjectID();
    }
    if (!createdOnServer) {
      return 0;
    } else {
      return this.serverMapManager.getAllSize(mapIDs);
    }
  }

  @Override
  public int getLocalSize() {
    if (!isCacheInitialized() || !localCacheEnabled) { return 0; }
    return this.cache.size();
  }

  private boolean isCacheInitialized() {
    if (this.cache == null) {
      logger.warn("Local cache yet not initialized");
      return false;
    }
    return true;
  }

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled.
   * 
   * @param map ServerTCMap
   */
  @Override
  public void clearLocalCache(final TCServerMap map) {
    lockAll();
    try {
      if (!isCacheInitialized()) { return; }
      this.cache.clear();
    } finally {
      unlockAll();
    }
  }

  @Override
  public void cleanLocalState() {
    lockAll();
    try {
      this.cache.cleanLocalState();
    } finally {
      unlockAll();
    }
  }

  @Override
  public void evictedInServer(Object key) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      // MNK-2875: Since server side eviction messages come in asynchronously with the initialization process, it's
      // possible to get eviction messages prior to the TCObjectServerMap being fully initialized. If that happens, we
      // can
      // just safely ignore any local cache removals since the local cache is uninitialized and thus empty.
      if (!isCacheInitialized()) { return; }
      this.cache.evictedInServer(key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void clearAllLocalCacheInline() {
    lockAll();
    try {
      if (!isCacheInitialized()) { return; }
      this.cache.clearInline();
    } finally {
      unlockAll();
    }
  }

  @Override
  public Set getLocalKeySet() {
    return isCacheInitialized() ? this.cache.getKeys() : Collections.emptySet();
  }

  @Override
  public boolean containsLocalKey(final Object key) {
    return isCacheInitialized() && this.localCacheEnabled && this.cache.getLocalValue(key) != null;
  }

  @Override
  public Object getValueFromLocalCache(final Object key) {
    if (!isCacheInitialized()) { return null; }
    AbstractLocalCacheStoreValue cachedItem = this.cache.getLocalValue(key);
    if (cachedItem != null) {
      return cachedItem.getValueObject();
    } else {
      return null;
    }
  }

  @Override
  public void removeValueFromLocalCache(final Object key) {
    if (isCacheInitialized()) {
      cache.removeFromLocalCache(key);
    }
  }

  private ObjectID invokeLogicalPut(final Object key, final Object value) {
    return invokeLogicalPutInternal(key, value, false);
  }

  private ObjectID invokeLogicalPutVersioned(final Object key, final Object value, final long version) {
    shareObject(key);
    final ObjectID valueObjectID = shareObject(value);
    final Object[] parameters = constructParamsVersioned(key, value, version);

    logicalInvoke(LogicalOperation.PUT_VERSIONED, parameters);
    return valueObjectID;
  }

  private ObjectID invokeLogicalPutIfAbsentVersioned(final Object key, final Object value, final long version) {
    shareObject(key);
    final ObjectID valueObjectID = shareObject(value);
    final Object[] parameters = constructParamsVersioned(key, value, version);

    logicalInvoke(LogicalOperation.PUT_IF_ABSENT_VERSIONED, parameters);
    return valueObjectID;
  }

  private ObjectID invokeLogicalPutInternal(final Object key, final Object value, boolean putIfAbsent) {
    shareObject(key);
    final ObjectID valueObjectID = shareObject(value);
    final Object[] parameters = constructParams(key, value);

    if (putIfAbsent) {
      logicalInvoke(LogicalOperation.PUT_IF_ABSENT, parameters);
    } else {
      logicalInvoke(LogicalOperation.PUT, parameters);
    }

    return valueObjectID;
  }

  private Object[] constructParams(final Object key, final Object newValue) {
    return constructParamsInternal(key, newValue, null, DEFAULT_VERSION);
  }

  private Object[] constructParamsForReplace(final Object key, final Object newValue, final Object oldValue) {
    return constructParamsInternal(key, newValue, oldValue, DEFAULT_VERSION);
  }

  private Object[] constructParamsVersioned(final Object key, final Object newValue, final long version) {
    return constructParamsInternal(key, newValue, null, version);
  }

  private Object[] constructParamsInternal(final Object key, final Object newValue, final Object oldValue,
                                           final long version) {
    final List<Object> params = new ArrayList<Object>();
    params.add(key);
    if (oldValue != null) {
      params.add(oldValue);
    }
    params.add(newValue);
    if (newValue instanceof ExpirableMapEntry) {
      final ExpirableMapEntry expirableEntry = (ExpirableMapEntry) newValue;
      params.add(expirableEntry.getCreationTime());
      params.add(expirableEntry.getLastAccessedTime());
      params.add(expirableEntry.getTimeToIdle());
      params.add(expirableEntry.getTimeToLive());
    }
    if (version != DEFAULT_VERSION) {
      params.add(version);
    }
    return params.toArray();
  }

  private ObjectID shareObject(Object param) {
    boolean isLiteral = LiteralValues.isLiteralInstance(param);
    if (!isLiteral) {
      TCObject object = this.objectManager.lookupOrCreate(param, this.groupID);
      return object.getObjectID();
    }
    return ObjectID.NULL_ID;
  }

  private void invokeLogicalRemove(final Object key) {
    logicalInvoke(LogicalOperation.REMOVE, new Object[] { key });
  }

  private void invokeLogicalRemoveVersioned(final Object key, final long version) {
    logicalInvoke(LogicalOperation.REMOVE_VERSIONED, new Object[] { key, version });
  }

  private boolean invokeLogicalRemove(final Object key, final Object value) throws AbortedOperationException {
    return logicalInvokeWithResult(LogicalOperation.REMOVE_IF_VALUE_EQUAL, new Object[] { key, value });
  }

  private void invokeLogicalExpire(final Object key, final Object value) {
    logicalInvoke(LogicalOperation.EXPIRE_IF_VALUE_EQUAL, new Object[] { key, value });
  }

  @Override
  public void addMetaData(MetaDataDescriptor mdd) {
    this.objectManager.getTransactionManager().addMetaDataDescriptor(this, (MetaDataDescriptorInternal) mdd);
  }

  @Override
  public void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback) {
    // this is called from CDSMDso.__tc_managed(tco)
    this.callback = callback;
    this.serverMapLocalStore = serverMapLocalStore;
    setupLocalCache(serverMapLocalStore, callback);
  }

  @Override
  public void destroyLocalStore() {
    this.serverMapLocalStore = null;
    this.cache = null;
  }

  private void setupLocalCache(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback) {
    this.cache = globalLocalCacheManager.getOrCreateLocalCache(getObjectID(), objectManager, manager,
                                                               localCacheEnabled, serverMapLocalStore, callback);
  }

  @Override
  public long getLocalOnHeapSizeInBytes() {
    return isCacheInitialized() ? this.cache.onHeapSizeInBytes() : 0;
  }

  @Override
  public long getLocalOffHeapSizeInBytes() {
    return isCacheInitialized() ? this.cache.offHeapSizeInBytes() : 0;
  }

  @Override
  public int getLocalOnHeapSize() {
    if (!isCacheInitialized() || !localCacheEnabled) { return 0; }
    return this.cache.onHeapSize();
  }

  @Override
  public int getLocalOffHeapSize() {
    return isCacheInitialized() ? this.cache.offHeapSize() : 0;
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return isCacheInitialized() && this.cache.containsKeyOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return isCacheInitialized() && this.cache.containsKeyOffHeap(key);
  }

  @Override
  public void setLocalCacheEnabled(boolean enabled) {
    this.localCacheEnabled = enabled;
    this.cache.setLocalCacheEnabled(enabled);
  }

  @Override
  public void recalculateLocalCacheSize(Object key) {
    if (isCacheInitialized()) {
      this.cache.recalculateSize(key);
    }
  }

  @Override
  public void doLogicalSetLastAccessedTime(final Object key, final Object value, final long lastAccessedTime) {
    logicalInvoke(LogicalOperation.SET_LAST_ACCESSED_TIME, new Object[] { key, value, lastAccessedTime });
  }

  private void lockAll() {
    for (Lock localLock : localLocks) {
      localLock.lock();
    }
  }

  private void unlockAll() {
    for (Lock localLock : localLocks) {
      localLock.unlock();
    }
  }

  private Lock getLockForKey(Object key) {
    Preconditions.checkNotNull(key, "Key cannot be null");
    return localLocks[Math.abs(spreadHash(key.hashCode()) % localLocks.length)];
  }

  private static int spreadHash(int h) {
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  @Override
  public void addTxnInProgressKeys(Set addSet, Set removeSet) {
    this.cache.addTxnInProgressKeys(addSet, removeSet);
  }

  @Override
  public void doRegisterListener(Set<ServerEventType> eventTypes, boolean skipRejoinChecks) {
    Set<Object> params = new HashSet<Object>();
    for (ServerEventType eventType : eventTypes) {
      params.add(eventType.ordinal());
    }

    // TODO: How to get the clientID here???
    logicalInvoke(LogicalOperation.REGISTER_SERVER_EVENT_LISTENER, params.toArray());
  }

  @Override
  public void doUnregisterListener(Set<ServerEventType> eventTypes) {
    Set<Object> params = new HashSet<Object>();
    for (ServerEventType eventType : eventTypes) {
      params.add(eventType.ordinal());
    }

    // TODO: How to get the clientID here???
    logicalInvoke(LogicalOperation.UNREGISTER_SERVER_EVENT_LISTENER, params.toArray());
  }

}
