/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.locks.LockID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectCloneUtil;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TCObjectServerMapImpl<L> extends TCObjectLogical implements TCObject, TCObjectServerMap<L> {

  private final static TCLogger               logger                          = TCLogging
                                                                                  .getLogger(TCObjectServerMapImpl.class);

  private static final Object[]               NO_ARGS                         = new Object[] {};

  private static final long                   GET_VALUE_FOR_KEY_LOG_THRESHOLD = 10 * 1000;

  // The condition that can require a retry is transient so we could retry immediately, this throttle is here just to
  // prevent hammering the server
  private static final long                   RETRY_GET_VALUE_FOR_KEY_SLEEP   = 10;

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
  private volatile boolean                    invalidateOnChange;
  private volatile boolean                    localCacheEnabled;

  private volatile L1ServerMapLocalCacheStore serverMapLocalStore;
  private final TCObjectSelfStore             tcObjectSelfStore;
  final L1ServerMapLocalCacheManager          globalLocalCacheManager;

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
      setupLocalCache(serverMapLocalStore);
    }
  }

  public void initialize(final int maxTTISeconds, final int maxTTLSeconds, final int targetMaxTotalCount,
                         final boolean invalidateOnChangeFlag, final boolean localCacheEnabledFlag) {
    this.invalidateOnChange = invalidateOnChangeFlag;
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
   * @param map ServerTCMap
   * @param lockID Lock under which this change is made
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPut(final TCServerMap map, final L lockID, final Object key, final Object value) {
    invokeLogicalPut(map, key, value);
    addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value, MapOperationType.PUT);
  }

  public void doClear(TCServerMap map) {
    logicalInvoke(SerializationUtil.CLEAR, SerializationUtil.PUT_SIGNATURE, NO_ARGS);
  }

  /**
   * Does a logical put and updates the local cache but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPutUnlocked(TCServerMap map, Object key, Object value) {
    ObjectID valueObjectID = invokeLogicalPut(map, key, value);

    if (!invalidateOnChange) {
      addIncoherentValueToCache(key, value, valueObjectID, MapOperationType.PUT);
    } else {
      addEventualValueToCache(key, value, valueObjectID, MapOperationType.PUT);
    }
  }

  public boolean doLogicalPutIfAbsentUnlocked(TCServerMap map, Object key, Object value) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null && item.getValueObject(tcObjectSelfStore, cache) != null) {
      // Item already present
      return false;
    }

    ObjectID valueObjectID = invokeLogicalPutIfAbsent(map, key, value);

    if (!invalidateOnChange) {
      addIncoherentValueToCache(key, value, valueObjectID, MapOperationType.PUT);
    } else {
      addEventualValueToCache(key, value, valueObjectID, MapOperationType.PUT);
    }

    return true;
  }

  public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object current, Object newValue) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null && current != item.getValueObject(tcObjectSelfStore, cache)) {
      // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
      // and coz of DEV-5462
      return false;
    }
    ObjectID valueObjectID = invokeLogicalReplace(map, key, current, newValue);

    if (!invalidateOnChange) {
      addIncoherentValueToCache(key, newValue, valueObjectID, MapOperationType.PUT);
    } else {
      addEventualValueToCache(key, newValue, valueObjectID, MapOperationType.PUT);
    }

    return true;
  }

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is removed
   * @param key Key Object
   */
  public void doLogicalRemove(final TCServerMap map, final L lockID, final Object key) {
    invokeLogicalRemove(map, key);

    addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, null, MapOperationType.REMOVE);
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   */
  public void doLogicalRemoveUnlocked(TCServerMap map, Object key) {
    invokeLogicalRemove(map, key);

    if (!invalidateOnChange) {
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
   * @return
   */
  public boolean doLogicalRemoveUnlocked(TCServerMap map, Object key, Object value) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null && value != item.getValueObject(tcObjectSelfStore, cache)) {
      // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
      // and coz of DEV-5462
      return false;
    }

    invokeLogicalRemove(map, key, value);

    if (!invalidateOnChange) {
      addIncoherentValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
    } else {
      addEventualValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
    }

    return true;
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @param lockID Lock under which this call is made
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValue(final TCServerMap map, final L lockID, final Object key) {
    final AbstractLocalCacheStoreValue item = this.cache.getCoherentLocalValue(key);
    if (item != null) { return item.getValueObject(tcObjectSelfStore, cache); }

    final Object value = getValueForKeyFromServer(map, key, false);
    if (value != null) {
      addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value, MapOperationType.GET);
    }

    return value;
  }

  public AddToCacheReturnType updateLocalCacheIfNecessary(final Object key, final Object value) {
    if (invalidateOnChange) {
      // Null values (i.e. cache misses) & literal values are not cached locally
      if (value != null && !LiteralValues.isLiteralInstance(value)) {
        //
        return addEventualValueToCache(key, value, this.objectManager.lookupExistingObjectID(value),
                                       MapOperationType.GET);
      } else {
        return AddToCacheReturnType.ADD_OK;
      }
    } else {
      return addIncoherentValueToCache(key, value, this.objectManager.lookupExistingObjectID(value),
                                       MapOperationType.GET);
    }
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap outside a lock context.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValueUnlocked(TCServerMap map, Object key) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null) return item.getValueObject(tcObjectSelfStore, cache);
    Object value = getValueForKeyFromServer(map, key, true);
    if (value != null) {
      AddToCacheReturnType addType = updateLocalCacheIfNecessary(key, value);
      if (addType == AddToCacheReturnType.ADD_IN_PROGRESS) {
        value = ObjectCloneUtil.clone(value);
      }
    }
    return value;
  }

  public Map<Object, Object> getAllValuesUnlocked(final Map<ObjectID, Set<Object>> mapIdToKeysMap) {
    Map<Object, Object> rv = new HashMap<Object, Object>();
    for (Iterator<Entry<ObjectID, Set<Object>>> iterator = mapIdToKeysMap.entrySet().iterator(); iterator.hasNext();) {
      Entry<ObjectID, Set<Object>> entry = iterator.next();
      Set<Object> keys = entry.getValue();
      for (Iterator i = keys.iterator(); i.hasNext();) {
        Object key = i.next();
        AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
        if (item != null) {
          i.remove();
          rv.put(key, item.getValueObject(tcObjectSelfStore, cache));
        }
      }
      if (keys.isEmpty()) {
        iterator.remove();
      }
    }

    // if everything was in local cache
    if (mapIdToKeysMap.isEmpty()) return rv;

    getAllValuesForKeyFromServer(mapIdToKeysMap, rv);

    return rv;

  }

  private AbstractLocalCacheStoreValue getValueUnlockedFromCache(Object key) {
    if (invalidateOnChange) {
      return this.cache.getCoherentLocalValue(key);
    } else {
      return this.cache.getLocalValue(key);
    }
  }

  public AddToCacheReturnType addStrongValueToCache(LockID lockId, Object key, Object value,
                                                    MapOperationType mapOperation) {
    Object valueToAdd = value instanceof TCObjectSelf ? ((TCObjectSelf) value).getObjectID() : value;
    final LocalCacheStoreStrongValue localCacheValue = new LocalCacheStoreStrongValue(lockId, valueToAdd, this.objectID);
    return addToCache(key, localCacheValue, value, mapOperation);
  }

  public AddToCacheReturnType addEventualValueToCache(Object key, Object value, ObjectID valueObjectID,
                                                      MapOperationType mapOperation) {
    final LocalCacheStoreEventualValue localCacheValue = new LocalCacheStoreEventualValue(valueObjectID, value,
                                                                                          this.objectID);
    return addToCache(key, localCacheValue, value, mapOperation);
  }

  public AddToCacheReturnType addIncoherentValueToCache(Object key, Object value, ObjectID valueObjectID,
                                                        MapOperationType mapOperation) {
    Object valueToAdd = value instanceof TCObjectSelf ? valueObjectID : value;
    final LocalCacheStoreIncoherentValue localCacheValue = new LocalCacheStoreIncoherentValue(valueToAdd, this.objectID);
    return addToCache(key, localCacheValue, value, mapOperation);
  }

  private AddToCacheReturnType addToCache(Object key, final AbstractLocalCacheStoreValue localCacheValue, Object value,
                                          MapOperationType mapOperation) {
    boolean notifyServerForRemove = false;
    if (value instanceof TCObjectSelf) {
      if (localCacheEnabled || mapOperation.isMutateOperation()) {
        if (!this.tcObjectSelfStore.addTCObjectSelf(serverMapLocalStore, localCacheValue, value,
                                                    mapOperation.isMutateOperation())) { return AddToCacheReturnType.ADD_IN_PROGRESS; }
      } else {
        notifyServerForRemove = true;
      }
    }
    // TODO: the check for local cache disabled and mutate op can be done here only
    cache.addToCache(key, localCacheValue, mapOperation);
    if (value instanceof TCObjectSelf) {
      this.tcObjectSelfStore.removeTCObjectSelfTemp((TCObjectSelf) value, notifyServerForRemove);
    }
    return AddToCacheReturnType.ADD_OK;
  }

  private Object getValueForKeyFromServer(final TCServerMap map, final Object key, final boolean retry) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getValueForKeyInMap is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    Object portableKey = getPortableKey(key);

    // If the key->value mapping is changed in some way during this lookup, it's possible that the value object
    // originally pointed to was deleted by DGC. If that happens we retry until we get a good value.
    long start = System.nanoTime();
    while (true) {
      final Object value = this.serverMapManager.getMappingForKey(mapID, portableKey);
      try {
        return lookupValue(value);
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

  private Object lookupValue(final Object value) throws TCObjectNotFoundException {
    if (value instanceof ObjectID) {
      try {
        return this.objectManager.lookupObjectQuiet((ObjectID) value);
      } catch (final ClassNotFoundException e) {
        logger.warn("Got ClassNotFoundException for objectId: " + value + ". Ignoring exception and returning null");
        return null;
      }
    } else {
      return value;
    }
  }

  private TCObjectServerMapImpl lookupTCObjectServerMapImpl(final ObjectID mapID) {
    try {
      return (TCObjectServerMapImpl) this.objectManager.lookup(mapID);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("ClassNotFoundException for mapID " + mapID);
    }
  }

  private Set<Object> getAllPortableKeys(final Set<Object> keys) {
    Set<Object> portableKeys = new HashSet<Object>();
    for (Object key : keys) {
      portableKeys.add(getPortableKey(key));
    }
    return portableKeys;
  }

  private void getAllValuesForKeyFromServer(final Map<ObjectID, Set<Object>> mapIdToKeysMap, Map<Object, Object> rv) {
    final Map<ObjectID, Set<Object>> mapIdsToLookup = new HashMap<ObjectID, Set<Object>>();
    for (Entry<ObjectID, Set<Object>> entry : mapIdToKeysMap.entrySet()) {
      // converting Map from <mapID, key> to <mapID, portableKey>
      mapIdsToLookup.put(entry.getKey(), getAllPortableKeys(entry.getValue()));
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
          Object value = rv.get(key);
          try {
            value = lookupValue(value);
          } catch (TCObjectNotFoundException e) {
            // We weren't able to find this particular mapping, continue for now, and try again on another pass
            continue;
          }
          portableKeyIterator.remove();

          // update the local cache of corresponding TCServerMap
          map.updateLocalCacheIfNecessary(key, value);
          rv.put(key, value);
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
  public Set keySet(final TCServerMap map) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException("keySet is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    return this.serverMapManager.getAllKeys(mapID);
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
   */
  public long getAllSize(final TCServerMap[] maps) {
    final ObjectID[] mapIDs = new ObjectID[maps.length];
    for (int i = 0; i < maps.length; ++i) {
      TCServerMap map = maps[i];
      final TCObject tcObject = map.__tc_managed();
      if (tcObject == null) { throw new UnsupportedOperationException(
                                                                      "getSize is not supported in a non-shared ServerMap"); }
      mapIDs[i] = tcObject.getObjectID();
    }
    return this.serverMapManager.getAllSize(mapIDs);
  }

  public int getLocalSize() {
    return this.cache.size();
  }

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled.
   * 
   * @param map ServerTCMap
   */
  public void clearLocalCache(final TCServerMap map) {
    this.cache.clear();
  }

  public void removeFromLocalCache(Object key) {
    // MNK-2875: Since server side eviction messages come in asynchronously with the initialization process, it's
    // possible to get eviction messages prior to the TCObjectServerMap being fully initialized. If that happens, we can
    // just safely ignore any local cache removals since the local cache is uninitialized and thus empty.
    if (this.cache != null) {
      this.cache.removeFromLocalCache(key);
    }
  }

  public void clearAllLocalCacheInline(final TCServerMap map) {
    this.cache.clearInline();
  }

  @Override
  protected boolean isEvictable() {
    return true;
  }

  /**
   * Called by the memory manager
   */
  @Override
  protected int clearReferences(final Object pojo, final int toClear) {
    // if this method is called, means there is a bug in the code, throwing AssertionError
    throw new AssertionError("we DO NOT want L1 cache manager to handle unclustered ehcache eviction");
  }

  public Set getLocalKeySet() {
    if (this.cache != null) { return this.cache.getKeySet(); }
    return Collections.EMPTY_SET;
  }

  public boolean containsLocalKey(final Object key) {
    if (this.localCacheEnabled) {
      return this.cache.getLocalValue(key) != null;
    } else {
      return false;
    }
  }

  public Object getValueFromLocalCache(final Object key) {
    AbstractLocalCacheStoreValue cachedItem = this.cache.getLocalValue(key);
    if (cachedItem != null) {
      return cachedItem.getValueObject(tcObjectSelfStore, cache);
    } else {
      return null;
    }
  }

  /**
   * Shares the entry and calls logicalPut.
   * 
   * @return ObjectID of the value
   */
  private ObjectID invokeLogicalPut(final TCServerMap map, final Object key, final Object value) {
    return invokeLogicalPutInternal(map, key, value, false);
  }

  /**
   * Shares the entry and calls logicalPutIfAbsent.
   * 
   * @return ObjectID of the value
   */
  private ObjectID invokeLogicalPutIfAbsent(final TCServerMap map, final Object key, final Object value) {
    return invokeLogicalPutInternal(map, key, value, true);
  }

  private ObjectID invokeLogicalPutInternal(TCServerMap map, Object key, Object value, boolean putIfAbsent) {
    final Object[] parameters = new Object[] { key, value };

    shareObject(key);
    ObjectID valueObjectID = shareObject(value);

    if (putIfAbsent) {
      logicalInvoke(SerializationUtil.PUT_IF_ABSENT, SerializationUtil.PUT_IF_ABSENT_SIGNATURE, parameters);
    } else {
      logicalInvoke(SerializationUtil.PUT, SerializationUtil.PUT_SIGNATURE, parameters);
    }
    return valueObjectID;
  }

  private ObjectID invokeLogicalReplace(final TCServerMap map, final Object key, final Object current,
                                        final Object newValue) {
    final Object[] parameters = new Object[] { key, current, newValue };

    shareObject(key);
    shareObject(current);
    ObjectID valueObjectID = shareObject(newValue);

    logicalInvoke(SerializationUtil.REPLACE_IF_VALUE_EQUAL, SerializationUtil.REPLACE_IF_VALUE_EQUAL_SIGNATURE,
                  parameters);
    return valueObjectID;
  }

  private ObjectID shareObject(Object param) {
    boolean isLiteral = LiteralValues.isLiteralInstance(param);
    if (!isLiteral) {
      TCObject object = this.objectManager.lookupOrCreate(param, this.groupID);
      return object.getObjectID();
    }
    return ObjectID.NULL_ID;
  }

  private void invokeLogicalRemove(final TCServerMap map, final Object key) {
    logicalInvoke(SerializationUtil.REMOVE, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { key });
  }

  private void invokeLogicalRemove(final TCServerMap map, final Object key, final Object value) {
    logicalInvoke(SerializationUtil.REMOVE_IF_VALUE_EQUAL, SerializationUtil.REMOVE_IF_VALUE_EQUAL_SIGNATURE,
                  new Object[] { key, value });
  }

  public void addMetaData(MetaDataDescriptor mdd) {
    this.objectManager.getTransactionManager().addMetaDataDescriptor(this, (MetaDataDescriptorInternal) mdd);
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore) {
    // this is called from CDSMDso.__tc_managed(tco)
    this.serverMapLocalStore = serverMapLocalStore;
    setupLocalCache(serverMapLocalStore);
  }

  public void destroyLocalStore() {
    this.serverMapLocalStore = null;
    this.cache = null;
  }

  private void setupLocalCache(L1ServerMapLocalCacheStore serverMapLocalStore) {
    this.cache = globalLocalCacheManager.getOrCreateLocalCache(this.objectID, objectManager, manager,
                                                               localCacheEnabled, serverMapLocalStore);
  }

  public long getLocalOnHeapSizeInBytes() {
    return this.cache.onHeapSizeInBytes();
  }

  public long getLocalOffHeapSizeInBytes() {
    return this.cache.offHeapSizeInBytes();
  }

  public int getLocalOnHeapSize() {
    return this.cache.onHeapSize();
  }

  public int getLocalOffHeapSize() {
    return this.cache.offHeapSize();
  }

  public boolean containsKeyLocalOnHeap(Object key) {
    if (this.cache.containsKeyOnHeap(key)) {
      AbstractLocalCacheStoreValue cacheStoreValue = this.cache.getLocalValue(key);
      if (cacheStoreValue == null) { return false; }
      return cacheStoreValue.isValueObjectOnHeap(serverMapLocalStore);
    }
    return false;
  }

  public boolean containsKeyLocalOffHeap(Object key) {
    return this.cache.containsKeyOffHeap(key);
  }

  private static enum AddToCacheReturnType {
    ADD_OK, ADD_IN_PROGRESS;
  }
}
