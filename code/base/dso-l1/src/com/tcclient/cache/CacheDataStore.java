/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.logging.TCLogger;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tc.util.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The main class for the cache. It holds CacheData objects
 */
public class CacheDataStore implements Serializable {
  // Resources
  private static final TCLogger              logger = ManagerUtil.getLogger("com.tc.cache.CacheDataStore");

  // Config
  private final CacheConfig                  config;

  // Cache state, changes during lifetime
  private final Map[]                        store;                                                        // <Object,
  // CacheData>,
  // values
  // may be
  // faulted
  // out
  private final Map[]                        dtmStore;                                                     // <Object,
  // Timestamp>,
  // values
  // never
  // faulted
  // out
  private final GlobalKeySet[]               globalKeySet;

  // Local cache stats
  private transient int                      hitCount;
  private transient int                      missCountExpired;
  private transient int                      missCountNotFound;

  // Local eviction thread
  private transient CacheInvalidationTimer[] cacheInvalidationTimer;

  /**
   * This is a shared object, so this only happens once. In other nodes, the initialize() method is called on load of
   * this object into the node.
   */
  public CacheDataStore(CacheConfig config) {
    this.config = config;

    // Set up cache state
    this.store = new Map[config.getConcurrency()];
    this.dtmStore = new Map[config.getConcurrency()];
    initializeStore();

    this.globalKeySet = new GlobalKeySet[config.getEvictorPoolSize()];
    initializeGlobalKeySet();

    this.hitCount = 0;
  }

  public CacheConfig getConfig() {
    return this.config;
  }

  private void initializeGlobalKeySet() {
    for (int i = 0; i < config.getEvictorPoolSize(); i++) {
      globalKeySet[i] = new GlobalKeySet();
    }
  }

  private void initializeStore() {
    for (int i = 0; i < config.getConcurrency(); i++) {
      this.store[i] = new HashMap();
      this.dtmStore[i] = new HashMap();
      ((Clearable) dtmStore[i]).setEvictionEnabled(false);
    }
  }

  /**
   * this method is added for backward compatibility with non partitioned clustered-ehcache that initializes
   * CacheDataStore using this method as onload function. In partitioned ehcache in forge, this method is not required
   * as CacheDataStore is initialized explicitly whenever CacheTC is accessed first time in a cluster node.
   */
  public void initialize() {
    this.initialize(ManagerUtil.getManager());
  }

  /**
   * Called onload to initialize transient per-node state
   */
  public void initialize(Manager manager) {
    logDebug("Initializing CacheDataStore");

    int startEvictionIndex = 0;
    this.cacheInvalidationTimer = new CacheInvalidationTimer[config.getEvictorPoolSize()];
    for (int i = 0; i < config.getEvictorPoolSize(); i++) {
      int lastEvictionIndex = startEvictionIndex + config.getStoresPerInvalidator();
      cacheInvalidationTimer[i] = new CacheInvalidationTimer(config.getInvalidatorSleepSeconds(),
                                                             config.getCacheName() + " invalidation thread" + i);

      cacheInvalidationTimer[i].start(new CacheEntryInvalidator(globalKeySet[i], startEvictionIndex, lastEvictionIndex,
                                                                config, manager, this), manager);
      startEvictionIndex = lastEvictionIndex;
    }
  }

  /**
   * This is used as a DistributedMethod because when one node cancel a timer, other nodes need to cancel the timer as
   * well.
   */
  public void stopInvalidatorThread() {
    logDebug("stopInvalidatorThread()");

    for (int i = 0; i < config.getEvictorPoolSize(); i++) {
      if (cacheInvalidationTimer[i] != null) {
        cacheInvalidationTimer[i].stop();
      }
    }
  }

  private int getStoreIndex(Object key) {
    return Util.hash(key, config.getConcurrency());
  }

  private CacheData putInternal(final Object key, final Object value) {
    logDebug("Put [" + key + ", " + value + "]");
    Assert.pre(key != null);
    Assert.pre(value != null);

    CacheData cd = new CacheData(value, config);
    cd.accessed();
    int storeIndex = getStoreIndex(key);

    CacheData rcd = (CacheData) store[storeIndex].put(key, cd);
    // Only need to put into the timestamp map only when the invalidator thread will be active
    if (config.getInvalidatorSleepSeconds() >= 0) {
      dtmStore[storeIndex].put(key, cd.getTimestamp());
    }
    return rcd;
  }

  public Object put(final Object key, final Object value) {
    CacheData rcd = putInternal(key, value);

    return ((rcd == null) ? null : rcd.getValue());
  }

  public void putData(final Object key, final Object value) {
    putInternal(key, value);
  }

  // private void dumpStore() {
  // for (int i = 0; i < config.getConcurrency(); i++) {
  // System.err.println("Dump store Client " + manager.getClientID() + "i: " + i + " " + store[i]);
  // }
  // }

  public Object get(final Object key) {
    Assert.pre(key != null);

    CacheData cd = null;
    cd = findCacheDataUnlocked(key);
    logDebug("Client " + ManagerUtil.getClientID() + " get [" + key + "] " + cd);
    if (cd != null) {
      if (!cd.isValid()) {
        missCountExpired++;
        invalidate(key, cd);
        return null;
      } else {
        hitCount++;
        cd.accessed();
        updateTimestampIfNeeded(key, cd);
      }
      return cd.getValue();
    }
    missCountNotFound++;
    return null;
  }

  private void invalidate(Object key, CacheData cd) {
    int storeIndex = getStoreIndex(key);
    if (!cd.isInvalidated()) {
      ManagerUtil.monitorEnter(store[storeIndex], LockLevel.CONCURRENT);
      try {
        cd.invalidate();
      } finally {
        ManagerUtil.monitorExit(store[storeIndex]);
      }
    }
  }

  public boolean isExpired(final Object key) {
    CacheData rv = findCacheDataUnlocked(key);
    logDebug("Checking isExpired for key: " + key + " " + rv);
    return rv == null || !rv.isValid();
  }

  public Object remove(final Object key) {
    CacheData cd = findCacheDataUnlocked(key);
    if (cd == null) return null;
    removeInternal(key);
    return cd.getValue();
  }

  private void removeInternal(final Object key) {
    Assert.pre(key != null);

    int storeIndex = getStoreIndex(key);
    ((TCMap) store[storeIndex]).__tc_remove_logical(key);
    ((TCMap) dtmStore[storeIndex]).__tc_remove_logical(key);
  }

  public void expire(Object key) {
    removeInternal(key);
    config.getCallback().expire(key);
  }

  public void clear() {
    for (int i = 0; i < config.getConcurrency(); i++) {
      store[i].clear();
      dtmStore[i].clear();
    }
  }

  public Map getStore(Object key) {
    int storeIndex = getStoreIndex(key);
    return store[storeIndex];
  }

  public Set entrySet() {
    Set entrySet = new HashSet();
    for (int i = 0; i < config.getConcurrency(); i++) {
      entrySet.addAll(store[i].entrySet());
    }
    return entrySet;
  }

  public boolean isEmpty() {
    for (int i = 0; i < config.getConcurrency(); i++) {
      if (!store[i].isEmpty()) { return false; }
    }
    return true;
  }

  public Set keySet() {
    Set keySet = new HashSet();
    for (int i = 0; i < config.getConcurrency(); i++) {
      Collection entrySnapshot = ((TCMap) store[i]).__tc_getAllEntriesSnapshot();
      for (Iterator it = entrySnapshot.iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        keySet.add(entry.getKey());
      }
    }
    return keySet;
  }

  public boolean containsValue(Object value) {
    CacheData cd = new CacheData(value, config);
    for (int i = 0; i < config.getConcurrency(); i++) {
      if (store[i].containsValue(cd)) { return true; }
    }
    return false;
  }

  public int size() {
    int size = 0;
    for (int i = 0; i < config.getConcurrency(); i++) {
      size += store[i].size();
    }
    return size;
  }

  public Collection values() {
    List values = new ArrayList();
    for (int i = 0; i < config.getConcurrency(); i++) {
      Collection entrySnapshot = ((TCMap) store[i]).__tc_getAllEntriesSnapshot();
      for (Iterator it = entrySnapshot.iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        values.add(entry.getValue());
      }
    }
    return values;
  }

  void updateTimestampIfNeeded(Object key, CacheData rv) {
    if (config.getMaxTTLSeconds() <= 0) { return; }

    Assert.pre(rv != null);
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    if (needsUpdate(rv)) {
      int storeIndex = getStoreIndex(key);
      ManagerUtil.monitorEnter(store[storeIndex], LockLevel.CONCURRENT);
      try {
        t.setExpiredTimeMillis(now + config.getMaxIdleTimeoutMillis());
      } finally {
        ManagerUtil.monitorExit(store[storeIndex]);
      }
    }
  }

  boolean needsUpdate(CacheData rv) {
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    final long diff = t.getExpiredTimeMillis() - now;
    return (diff < (config.getMaxIdleTimeoutMillis() / 2) || diff > (config.getMaxIdleTimeoutMillis()));
  }

  Timestamp findTimestampUnlocked(final Object key) {
    int storeIndex = getStoreIndex(key);
    return (Timestamp) dtmStore[storeIndex].get(key);
  }

  CacheData findCacheDataUnlocked(final Object key) {
    int storeIndex = getStoreIndex(key);
    final CacheData rv = (CacheData) store[storeIndex].get(key);
    return rv;
  }

  public int getHitCount() {
    return hitCount;
  }

  public int getMissCountExpired() {
    return missCountExpired;
  }

  public int getMissCountNotFound() {
    return missCountNotFound;
  }

  public void clearStatistics() {
    this.hitCount = 0;
    this.missCountExpired = 0;
    this.missCountNotFound = 0;
  }

  private void logDebug(String msg) {
    if (config.isLoggingEnabled()) {
      if (DebugUtil.DEBUG) {
        System.err.println(msg);
      }
      logger.debug(msg);
    }
  }

  private void logError(String msg, Throwable t) {
    if (config.isLoggingEnabled()) {
      logger.error(msg, t);
    }
  }

  private Collection getAllLocalEntries(int startEvictionIndex, int lastEvictionIndex) {
    Collection allLocalEntries = new ArrayList();
    for (int i = startEvictionIndex; i < lastEvictionIndex; i++) {
      Collection t = ((TCMap) dtmStore[i]).__tc_getAllLocalEntriesSnapshot();
      allLocalEntries.addAll(t);
    }
    return allLocalEntries;
  }

  Object[] getAllLocalKeys(int startEvictionIndex, int lastEvictionIndex) {
    Collection allLocalEntries = getAllLocalEntries(startEvictionIndex, lastEvictionIndex);
    Object[] allLocalKeys = new Object[allLocalEntries.size()];
    int i = 0;
    for (Iterator it = allLocalEntries.iterator(); it.hasNext(); i++) {
      Map.Entry e = (Map.Entry) it.next();
      allLocalKeys[i] = e.getKey();
    }
    return allLocalKeys;
  }

  private Collection getAllOrphanEntries(Collection remoteKeys, int startEvictionIndex, int lastEvictionIndex) {
    Collection allEntries = new ArrayList();
    for (int i = startEvictionIndex; i < lastEvictionIndex; i++) {
      Collection t = ((TCMap) dtmStore[i]).__tc_getAllEntriesSnapshot();
      allEntries.addAll(t);
    }
    for (Iterator it = allEntries.iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      if (remoteKeys.contains(e.getKey())) {
        it.remove();
      }
    }
    return allEntries;
  }

  public void evictExpiredElements() {
    evictExpiredElements(0, config.getConcurrency());
  }

  public void evictExpiredElements(int startEvictionIndex, int lastEvictionIndex) {
    final Collection localEntries = getAllLocalEntries(startEvictionIndex, lastEvictionIndex);
    invalidateCacheEntries(localEntries, false, -1, -1);
  }

  public void evictAllExpiredElements(Collection remoteKeys, int startEvictionIndex, int lastEvictionIndex) {
    final Collection orphanEntries = getAllOrphanEntries(remoteKeys, startEvictionIndex, lastEvictionIndex);
    invalidateCacheEntries(orphanEntries, true, config.getNumOfChunks(), config.getRestMillis());
  }

  private void invalidateCacheEntries(final Collection entriesToBeExamined, boolean isGlobalInvalidation,
                                      int numOfChunks, long restMillis) {
    int totalCnt = 0;
    int evaled = 0;
    int notEvaled = 0;
    int errors = 0;
    long numOfObjectsPerChunk = entriesToBeExamined.size();

    if (isGlobalInvalidation) {
      // Use ceiling here so that we get at least 1 obj / chunk
      numOfObjectsPerChunk = (int) Math.ceil(entriesToBeExamined.size() * 1.0 / numOfChunks);
    }

    for (Iterator it = entriesToBeExamined.iterator(); it.hasNext();) {
      final Map.Entry timestampEntry = (Map.Entry) it.next();

      try {
        final Timestamp dtm = findTimestampUnlocked(timestampEntry.getKey());
        logDebug("checking validation key: " + timestampEntry.getKey() + ", dtm: " + dtm);
        if (dtm == null) continue;
        totalCnt++;
        if (dtm.getInvalidatedTimeMillis() < System.currentTimeMillis()) {
          evaled++;
          logDebug("expiring .... key: " + timestampEntry.getKey());
          expire(timestampEntry.getKey());
        } else {
          notEvaled++;
        }
      } catch (Throwable t) {
        errors++;
        t.printStackTrace(System.err);
        logError("Unhandled exception inspecting session " + timestampEntry.getKey() + " for invalidation", t);
      } finally {
        if (isGlobalInvalidation) {
          if ((totalCnt % numOfObjectsPerChunk) == 0) {
            try {
              Thread.sleep(restMillis);
            } catch (InterruptedException e) {
              // ignore
            }
          }
        }
      }
    }
  }

}
