/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.cache.ExpirableEntry;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.cache.CachedItem;
import com.tc.object.cache.CachedItem.CachedItemInitialization;
import com.tc.object.cache.IncoherentCachedItem;
import com.tc.object.locks.LockID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TCObjectServerMapImpl<L> extends TCObjectLogical implements TCObject, TCObjectServerMap<L> {

  private final static TCLogger        logger           = TCLogging.getLogger(TCObjectServerMapImpl.class);

  private static final boolean         EVICTOR_LOGGING  = TCPropertiesImpl
                                                            .getProperties()
                                                            .getBoolean(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED);
  private final static boolean         CACHE_ENABLED    = TCPropertiesImpl
                                                            .getProperties()
                                                            .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED);

  private static final Object[]        NO_ARGS          = new Object[] {};

  static {
    logger.info(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED + " : " + CACHE_ENABLED);
  }

  private final GroupID                groupID;
  private final ClientObjectManager    objectManager;
  private final RemoteServerMapManager serverMapManager;
  private final Manager                manager;
  private final LocalCache             cache            = new LocalCache();
  private volatile int                 maxInMemoryCount = 0;
  private volatile int                 tti              = 0;
  private volatile int                 ttl              = 0;
  private volatile boolean             invalidateOnChange;

  private volatile boolean             localCacheEnabled;

  public TCObjectServerMapImpl(final Manager manager, final ClientObjectManager objectManager,
                               final RemoteServerMapManager serverMapManager, final ObjectID id, final Object peer,
                               final TCClass tcc, final boolean isNew) {
    super(id, peer, tcc, isNew);
    this.groupID = new GroupID(id.getGroupID());
    this.objectManager = objectManager;
    this.serverMapManager = serverMapManager;
    this.manager = manager;
  }

  public void initialize(final int maxTTISeconds, final int maxTTLSeconds, final int targetMaxInMemoryCount,
                         final int targetMaxTotalCount, final boolean invalidateOnChangeFlag,
                         final boolean localCacheEnabledFlag) {
    this.maxInMemoryCount = targetMaxInMemoryCount;
    this.tti = maxTTISeconds;
    this.ttl = maxTTLSeconds;
    this.invalidateOnChange = invalidateOnChangeFlag;
    this.localCacheEnabled = localCacheEnabledFlag;
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

    if (CACHE_ENABLED) {
      this.cache.addCoherentValueToCache(this.manager.generateLockIdentifier(lockID), key, value, true);
    }
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
    ObjectID valueID = invokeLogicalPut(map, key, value);

    if (CACHE_ENABLED) {
      if (!invalidateOnChange || valueID.isNull()) {
        this.cache.addIncoherentValueToCache(key, value, true);
      } else {
        this.cache.addCoherentValueToCache(valueID, key, value, true);
      }
    }
  }

  public boolean doLogicalPutIfAbsentUnlocked(TCServerMap map, Object key, Object value) {
    if (CACHE_ENABLED) {
      CachedItem item = getValueUnlockedFromCache(key);
      if (item != null && item.getValue() != null) {
        // Item already present
        return false;
      }
    }

    ObjectID valueID = invokeLogicalPutIfAbsent(map, key, value);

    if (CACHE_ENABLED) {
      if (!invalidateOnChange || valueID.isNull()) {
        this.cache.addIncoherentValueToCache(key, value, true);
      } else {
        this.cache.addCoherentValueToCache(valueID, key, value, true);
      }
    }

    return true;
  }

  public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object current, Object newValue) {
    if (CACHE_ENABLED) {
      CachedItem item = getValueUnlockedFromCache(key);
      if (item != null && current != item.getValue()) {
        // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
        // and coz of DEV-5462
        return false;
      }
    }
    ObjectID valueID = invokeLogicalReplace(map, key, current, newValue);

    if (CACHE_ENABLED) {
      if (!invalidateOnChange || valueID.isNull()) {
        this.cache.addIncoherentValueToCache(key, newValue, true);
      } else {
        this.cache.addCoherentValueToCache(valueID, key, newValue, true);
      }
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

    if (CACHE_ENABLED) {
      this.cache.addCoherentValueToCache(this.manager.generateLockIdentifier(lockID), key, null, true);
    }
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   */
  public void doLogicalRemoveUnlocked(TCServerMap map, Object key) {
    invokeLogicalRemove(map, key);

    if (CACHE_ENABLED) {
      if (!invalidateOnChange) {
        this.cache.addIncoherentValueToCache(key, null, true);
      } else {
        this.cache.addCoherentValueToCache(ObjectID.NULL_ID, key, null, true);
      }
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
    if (CACHE_ENABLED) {
      CachedItem item = getValueUnlockedFromCache(key);
      if (item != null && value != item.getValue()) {
        // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
        // and coz of DEV-5462
        return false;
      }
    }

    invokeLogicalRemove(map, key, value);

    if (CACHE_ENABLED) {
      if (!invalidateOnChange) {
        this.cache.addIncoherentValueToCache(key, null, true);
      } else {
        this.cache.addCoherentValueToCache(ObjectID.NULL_ID, key, null, true);
      }
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
    if (CACHE_ENABLED) {
      final CachedItem item = this.cache.getCoherentCachedItem(key);
      if (item != null) { return item.getValue(); }
    }

    final Object value = getValueForKeyFromServer(map, key);
    if (CACHE_ENABLED) {
      this.cache.addCoherentValueToCache(this.manager.generateLockIdentifier(lockID), key, value, false);
    }

    return value;
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
    if (CACHE_ENABLED) {
      CachedItem item = getValueUnlockedFromCache(key);
      if (item != null) return item.getValue();
    }

    final Object value = getValueForKeyFromServer(map, key);

    if (CACHE_ENABLED) {
      if (invalidateOnChange) {
        // Null values (i.e. cache misses) & literal values are not cached locally
        if (value != null && !LiteralValues.isLiteralInstance(value)) {
          this.cache.addCoherentValueToCache(objectManager.lookupExistingObjectID(value), key, value, false);
        }
      } else {
        this.cache.addIncoherentValueToCache(key, value, false);
      }
    }
    return value;
  }

  private CachedItem getValueUnlockedFromCache(Object key) {
    if (invalidateOnChange) {
      return this.cache.getCoherentCachedItem(key);
    } else {
      return this.cache.getCachedItem(key);
    }
  }

  private Object getValueForKeyFromServer(final TCServerMap map, final Object key) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getValueForKeyInMap is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    Object portableKey = key;
    if (key instanceof Manageable) {
      final TCObject keyObject = ((Manageable) key).__tc_managed();
      if (keyObject == null) { throw new UnsupportedOperationException(
                                                                       "Key is portable, but not shared. This is currently not supported with ServerMap. Map ID = "
                                                                           + mapID + " key = " + key); }
      portableKey = keyObject.getObjectID();
    }

    if (!LiteralValues.isLiteralInstance(portableKey)) {
      // formatter
      throw new UnsupportedOperationException(
                                              "Key is not portable. It needs to be a liternal or portable and shared for ServerTCMap. Key = "
                                                  + portableKey + " map id = " + mapID);
    }

    final Object value = this.serverMapManager.getMappingForKey(mapID, portableKey);

    if (value instanceof ObjectID) {
      try {
        return this.objectManager.lookupObject((ObjectID) value);
      } catch (final ClassNotFoundException e) {
        logger.warn("Got ClassNotFoundException for objectId: " + value + ". Ignoring exception and returning null");
        return null;
      } catch (TCObjectNotFoundException e) {
        logger.warn("Got TCObjectNotFoundException for objectId: " + value + ". Ignoring exception and returning null");
        return null;
      }
    } else {
      return value;
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
    this.cache.clearAllLocalCache();
  }

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled. This method
   * will wait until lock recall is complete.
   * 
   * @param map ServerTCMap
   */
  public void clearAllLocalCacheInline(final TCServerMap map) {
    this.cache.inlineClearAllLocalCache();
  }

  public void removeFromLocalCache(Object key) {
    this.cache.removeFromLocalCache(key);
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
    if (this.maxInMemoryCount > 0) {
      // don't clear, let target capacity eviction handle this.
      return 0;
    } else {
      if (EVICTOR_LOGGING) {
        logEviction("Memory Manager requesting eviction: toClear=" + toClear);
      }
      return this.cache.evictCachedEntries(toClear);
    }
  }

  private void logEviction(final String msg) {
    logger.info("ServerMap Eviction: " + getObjectID() + " : " + msg);
  }

  public void doCapacityEviction() {
    this.cache.doCapacityEviction();
  }

  public Set getLocalKeySet() {
    if (CACHE_ENABLED) {
      return this.cache.getKeySet();
    } else {
      return Collections.EMPTY_SET;
    }
  }

  public boolean containsLocalKey(final Object key) {
    if (CACHE_ENABLED) {
      return this.cache.containsKey(key);
    } else {
      return false;
    }
  }

  public Object getValueFromLocalCache(final Object key) {
    if (CACHE_ENABLED) {
      CachedItem cachedItem = this.cache.getCachedItem(key);
      if (cachedItem != null) { return cachedItem.getValue(); }
    }
    return null;
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
    ObjectID valueID = shareObject(value);

    if (putIfAbsent) {
      logicalInvoke(SerializationUtil.PUT_IF_ABSENT, SerializationUtil.PUT_IF_ABSENT_SIGNATURE, parameters);
    } else {
      logicalInvoke(SerializationUtil.PUT, SerializationUtil.PUT_SIGNATURE, parameters);
    }
    return valueID;
  }

  private ObjectID invokeLogicalReplace(final TCServerMap map, final Object key, final Object current,
                                        final Object newValue) {
    final Object[] parameters = new Object[] { key, current, newValue };

    shareObject(key);
    shareObject(current);
    ObjectID valueID = shareObject(newValue);

    logicalInvoke(SerializationUtil.REPLACE_IF_VALUE_EQUAL, SerializationUtil.REPLACE_IF_VALUE_EQUAL_SIGNATURE,
                  parameters);
    return valueID;
  }

  private ObjectID shareObject(Object param) {
    boolean isLiteral = LiteralValues.isLiteralInstance(param);
    if (!isLiteral) {
      TCObject tcObject = this.objectManager.lookupOrCreate(param, this.groupID);
      return tcObject.getObjectID();
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

  /* Local Cache operations */
  private final class LocalCache implements CachedItem.DisposeListener {

    private static final int                            CACHE_ITEM_DISPOSE_LOGGING_INTERVAL_MILLIS = 5000;

    private final ConcurrentHashMap<Object, CachedItem> map                                        = new ConcurrentHashMap<Object, CachedItem>();
    private final AtomicInteger                         size                                       = new AtomicInteger();
    private final AtomicBoolean                         evictionInitiated                          = new AtomicBoolean();
    private Iterator<CachedItem>                        evictionPointer                            = this.map.values()
                                                                                                       .iterator();
    private final AtomicLong                            disposedCacheEntriesCount                  = new AtomicLong();
    private final AtomicLong                            lastLogTime                                = new AtomicLong(
                                                                                                                    System
                                                                                                                        .currentTimeMillis());

    /**
     * Creates a coherent mapping for the key to CachedItem
     * 
     * @param id - LockID that is protecting the item
     * @param key - key of the mapping
     * @param value - value of the mapping
     * @param b
     */
    public void addCoherentValueToCache(final LockID id, final Object key, final Object value, boolean isMutate) {
      addCoherentValueToCacheInternal(id, key, value, isMutate);
    }

    private void addCoherentValueToCacheInternal(Object id, Object key, Object value, boolean isMutate) {
      final CachedItem item;
      if (!localCacheEnabled) {
        item = new CachedItem(id, this, key, value, CachedItemInitialization.REMOVE_ON_TXN_COMPLETE);
      } else {
        item = new CachedItem(id, this, key, value, isMutate ? CachedItemInitialization.WAIT_FOR_ACK
            : CachedItemInitialization.NO_WAIT_FOR_ACK);
      }
      addToCache(key, item, isMutate);
    }

    private void registerForCallbackOnComplete(final CachedItem item) {
      ClientTransaction txn = TCObjectServerMapImpl.this.objectManager.getTransactionManager().getCurrentTransaction();
      if (txn == null) { throw new UnlockedSharedObjectException(
                                                                 "Attempt to access a shared object outside the scope of a shared lock.",
                                                                 Thread.currentThread().getName(), manager
                                                                     .getClientID()); }
      txn.addTransactionCompleteListener(item);
    }

    /**
     * Creates a coherent mapping for the key to CachedItem
     * 
     * @param id - an ObjectID that is mapped to the key
     * @param key - key of the mapping
     * @param value - value of the mapping
     */
    public void addCoherentValueToCache(final ObjectID id, final Object key, final Object value, boolean isMutate) {
      addCoherentValueToCacheInternal(id, key, value, isMutate);
    }

    public void addIncoherentValueToCache(final Object key, final Object value, boolean isMutate) {
      final IncoherentCachedItem item;
      if (!localCacheEnabled) {
        item = new IncoherentCachedItem(this, key, value, CachedItemInitialization.REMOVE_ON_TXN_COMPLETE);
      } else {
        item = new IncoherentCachedItem(this, key, value, isMutate ? CachedItemInitialization.WAIT_FOR_ACK
            : CachedItemInitialization.NO_WAIT_FOR_ACK);
      }
      addToCache(key, item, isMutate);
    }

    // TODO::FIXME:: There is a race for puts for same key from same vm - it races between the map.put() and
    // serverMapManager.put()
    private void addToCache(final Object key, final CachedItem item, boolean isMutate) {
      if (!localCacheEnabled && !isMutate) {
        // local cache NOT enabled AND NOT a mutate operation, do not cache anything locally
        // for mutate ops keep in local cache till txn is complete
        return;
      }
      final CachedItem old = this.map.put(key, item);
      int currentSize;
      if (old == null) {
        currentSize = this.size.incrementAndGet();
      } else {
        currentSize = this.size.get();
        Object oldID = old.getID();
        if (oldID != null && oldID != ObjectID.NULL_ID) {
          TCObjectServerMapImpl.this.serverMapManager.removeCachedItem(oldID, old);
        }
      }
      Object itemID = item.getID();
      if (itemID != null && itemID != ObjectID.NULL_ID) {
        TCObjectServerMapImpl.this.serverMapManager.addCachedItem(itemID, item);
      }
      initiateEvictionIfNecessary(currentSize);
      if (isMutate) {
        registerForCallbackOnComplete(item);
      }
    }

    private void initiateEvictionIfNecessary(final int currentSize) {
      if (TCObjectServerMapImpl.this.maxInMemoryCount <= 0
          || currentSize <= TCObjectServerMapImpl.this.maxInMemoryCount) { return; }
      if (currentSize > TCObjectServerMapImpl.this.maxInMemoryCount && !this.evictionInitiated.getAndSet(true)) {
        TCObjectServerMapImpl.this.serverMapManager.initiateCachedItemEvictionFor(TCObjectServerMapImpl.this);
      }
    }

    public void doCapacityEviction() {
      try {
        int currentSize;
        if (TCObjectServerMapImpl.this.maxInMemoryCount <= 0
            || (currentSize = size()) <= TCObjectServerMapImpl.this.maxInMemoryCount) { return; }
        // overshoot + 20 % of max
        final int toClear = (int) ((currentSize - TCObjectServerMapImpl.this.maxInMemoryCount) + (TCObjectServerMapImpl.this.maxInMemoryCount * 0.2));
        if (EVICTOR_LOGGING) {
          logEviction("Running capacity eviction. maxInMemoryCount=" + TCObjectServerMapImpl.this.maxInMemoryCount
                      + " currentSize=" + currentSize + " toClear=" + toClear);
        }
        evictCachedEntries(toClear);
      } finally {
        this.evictionInitiated.set(false);
      }
    }

    // Evict some entries from local cache, it initiates lock recalls
    public int evictCachedEntries(final int toClear) {
      int cleared = 0;
      final Set<LockID> toEvict = new HashSet<LockID>();
      int iterateCount = this.size.get();
      final int now = (int) (System.currentTimeMillis() / 1000);
      int expired = 0;
      int missed = 0;
      int incoherentItemsRemoved = 0;
      while (iterateCount-- > 0 && cleared < toClear) {
        final CachedItem ci = getNextEntryToInspect();
        if (ci == null) {
          break;
        } else if (ci.isExpired()) {
          continue;
        } else if (isExpired(ci, now)) {
          expire(ci);
          if (isIncoherent(ci)) {
            incoherentItemsRemoved++;
          } else {
            expired++;
          }
        } else if (!ci.getAndClearAccessed()) {
          if (isIncoherent(ci)) {
            // remove from map directly as not associated with any locks
            removeFromMap(ci.getKey());
            incoherentItemsRemoved++;
          } else if (isUnlockedCoherent(ci)) {
            // Directly flush
            TCObjectServerMapImpl.this.serverMapManager.flush(ci.getID());
          } else if (isLockedCoherent(ci)) {
            // could have lock collisions
            toEvict.add((LockID) ci.getID());
          } else {
            throw new AssertionError("Unknown CachedItem : " + ci);
          }
          cleared++;
        } else {
          missed++;
        }
      }
      if (EVICTOR_LOGGING) {
        logEviction(" ... tried to clear: " + toClear + " missed: " + missed + " expired: " + expired + " cleared: "
                    + cleared + " incoherent items removed: " + incoherentItemsRemoved);
      }
      if (!toEvict.isEmpty()) {
        TCObjectServerMapImpl.this.serverMapManager.clearCachedItemsForLocks(toEvict, false);
      }
      return cleared;
    }

    private void expire(CachedItem ci) {
      ci.markExpired();
      if (isIncoherent(ci)) {
        disposed(ci);
      } else if (isUnlockedCoherent(ci)) {
        // TODO::Make it expire (remove from cache) from L1 after implementing two arg remove at the server
        // For now flushing to server so server eviction can take care of this.
        TCObjectServerMapImpl.this.serverMapManager.flush(ci.getID());
      } else {
        TCObjectServerMapImpl.this.serverMapManager.expired(TCObjectServerMapImpl.this, ci);
      }
    }

    private boolean isExpired(final CachedItem ci, final int now) {
      final ExpirableEntry ee;
      if ((TCObjectServerMapImpl.this.tti <= 0 && TCObjectServerMapImpl.this.ttl <= 0)
          || (ee = ci.getExpirableEntry()) == null) { return false; }
      return now >= ee.expiresAt(TCObjectServerMapImpl.this.tti, TCObjectServerMapImpl.this.ttl);
    }

    private CachedItem getNextEntryToInspect() {
      if (this.evictionPointer.hasNext()) {
        return this.evictionPointer.next();
      } else {
        this.evictionPointer = this.map.values().iterator();
        return this.evictionPointer.hasNext() ? this.evictionPointer.next() : null;
      }
    }

    public void clearAllLocalCache() {
      Set<LockID> toClear = clearAllLocalCacheHelper();
      if (!toClear.isEmpty()) {
        TCObjectServerMapImpl.this.serverMapManager.clearCachedItemsForLocks(toClear, false);
      }
    }

    public void inlineClearAllLocalCache() {
      Set<LockID> toClear = clearAllLocalCacheHelper();
      if (!toClear.isEmpty()) {
        TCObjectServerMapImpl.this.serverMapManager.clearCachedItemsForLocks(toClear, true);
      }
    }

    private Set<LockID> clearAllLocalCacheHelper() {
      final Set<LockID> toClear = new HashSet<LockID>();
      for (final CachedItem ci : this.map.values()) {
        if (isIncoherent(ci)) {
          // Directly dispose these items as they are not mapped to ObjectID or LockID
          disposed(ci);
        } else if (isUnlockedCoherent(ci)) {
          // Directly flush these items
          TCObjectServerMapImpl.this.serverMapManager.flush(ci.getID());
        } else if (isLockedCoherent(ci)) {
          // These items are mapped to a LockID and they Locks need to be recalled.
          toClear.add((LockID) ci.getID());
        } else {
          throw new AssertionError("Unknown cached Item : " + ci);
        }
      }
      return toClear;
    }

    // TODO:: Code repetition, avoid it
    public void removeFromLocalCache(Object key) {
      CachedItem ci = getCachedItem(key);
      if (ci == null) return;
      if (isIncoherent(ci)) {
        disposed(ci);
      } else if (isUnlockedCoherent(ci)) {
        TCObjectServerMapImpl.this.serverMapManager.flush(ci.getID());
      } else if (isLockedCoherent(ci)) {
        TCObjectServerMapImpl.this.serverMapManager
            .clearCachedItemsForLocks(Collections.singleton((LockID) ci.getID()), false);
      } else {
        throw new AssertionError("Unknown cached Item : " + ci);
      }
    }

    public int size() {
      return this.size.get();
    }

    public void disposed(final CachedItem ci) {
      CachedItem removed = removeFromMap(ci.getKey());
      if (removed != null && EVICTOR_LOGGING) {
        this.disposedCacheEntriesCount.incrementAndGet();
        final long now = System.currentTimeMillis();
        final long prev = this.lastLogTime.get();
        if (now - prev >= CACHE_ITEM_DISPOSE_LOGGING_INTERVAL_MILLIS) {
          if (this.lastLogTime.compareAndSet(prev, now)) {
            final long disposedCount = this.disposedCacheEntriesCount.getAndSet(0);
            logEviction("Number of cache items disposed in last " + (now - prev) + " millis: " + disposedCount);
          }
        }
      }
    }

    public void evictFromLocalCache(final CachedItem ci) {
      if (removeFromMap(ci.getKey(), ci)) {
        TCObjectServerMapImpl.this.serverMapManager.removeCachedItem(ci.getID(), ci);
      }
    }

    private CachedItem removeFromMap(final Object key) {
      final CachedItem removed = this.map.remove(key);
      if (removed != null) {
        this.size.decrementAndGet();
      }
      return removed;
    }

    private boolean removeFromMap(final Object key, final CachedItem item) {
      if (this.map.remove(key, item)) {
        this.size.decrementAndGet();
        return true;
      }
      return false;
    }

    /**
     * Returned value may be coherent or incoherent or null
     */
    public CachedItem getCachedItem(final Object key) {
      CachedItem cachedItem = this.map.get(key);
      if (isIncoherent(cachedItem)) {
        // if incoherent and been incoherent too long, remove from cache/map
        if (((IncoherentCachedItem) cachedItem).isIncoherentTooLong()) {
          removeFromMap(key);
          return null;
        }
      }
      return cachedItem;
    }

    /**
     * Returned value is always coherent or null.
     */
    public CachedItem getCoherentCachedItem(final Object key) {
      final CachedItem item = getCachedItem(key);
      if (isIncoherent(item)) {
        removeFromMap(key);
        return null;
      }
      return item;
    }

    private boolean isIncoherent(final CachedItem item) {
      return item instanceof IncoherentCachedItem;
    }

    private boolean isUnlockedCoherent(CachedItem ci) {
      return ci.getID() instanceof ObjectID;
    }

    private boolean isLockedCoherent(CachedItem ci) {
      return ci.getID() instanceof LockID;
    }

    public Set getKeySet() {
      return Collections.unmodifiableSet(this.map.keySet());
    }

    public boolean containsKey(final Object key) {
      return this.map.containsKey(key);
    }
  }
}
