/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.config.lock.LockLevel;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;
import com.tc.properties.TCProperties;
import com.tc.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CacheDataStore implements Serializable {
  private final Map[]                        store;                  // <Data>
  private final Map[]                        dtmStore;               // <Timestamp>
  private final String                       cacheName;
  private final long                         maxIdleTimeoutSeconds;
  private final long                         maxTTLSeconds;
  private final long                         invalidatorSleepSeconds;
  private final GlobalKeySet[]               globalKeySet;
  private final Expirable                    callback;
  private int                                concurrency;
  private int                                evictorPoolSize;
  private boolean                            globalEvictionEnabled;
  private int                                globalEvictionFrequency;
  private transient CacheParticipants        cacheParticipants;
  private transient int                      hitCount;
  private transient int                      missCountExpired;
  private transient int                      missCountNotFound;
  private transient CacheInvalidationTimer[] cacheInvalidationTimer;
  
  
  private static int hash(int h) {
    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    h ^= (h >>> 10);
    return h;
  }

  public CacheDataStore(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, long maxTTLSeconds, String cacheName,
                        Expirable callback) {
    TCProperties ehcacheProperies = ManagerUtil.getTCProperties().getPropertiesFor("ehcache");
    this.concurrency = ehcacheProperies.getInt("concurrency");
    if (this.concurrency <= 1) {
      this.concurrency = 1;
      this.evictorPoolSize = 1;
    } else {
      this.evictorPoolSize = ehcacheProperies.getInt("evictor.pool.size");
      if (this.evictorPoolSize > this.concurrency) {
        this.evictorPoolSize = this.concurrency;
      }
    }

    this.store = new Map[concurrency];
    this.dtmStore = new Map[concurrency];
    this.globalKeySet = new GlobalKeySet[evictorPoolSize];

    this.cacheName = cacheName;
    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;
    this.maxTTLSeconds = maxTTLSeconds;
    this.invalidatorSleepSeconds = invalidatorSleepSeconds;
    this.callback = callback;
    this.hitCount = 0;

    this.globalEvictionEnabled = ehcacheProperies.getBoolean("global.eviction.enable");
    this.globalEvictionFrequency = ehcacheProperies.getInt("global.eviction.frequency");
    initializeGlobalKeySet();
    initializeStore();
  }

  // For test only
  public CacheDataStore(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, long maxTTLSeconds, String cacheName,
                        Expirable callback, boolean globalEvictionEnabled, int globalEvictionFrequency,
                        int concurrency, int evictorPoolSize) {
    this.concurrency = concurrency;
    this.evictorPoolSize = evictorPoolSize;

    this.store = new Map[concurrency];
    this.dtmStore = new Map[concurrency];
    this.globalKeySet = new GlobalKeySet[evictorPoolSize];

    this.cacheName = cacheName;
    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;
    this.maxTTLSeconds = maxTTLSeconds;
    this.invalidatorSleepSeconds = invalidatorSleepSeconds;
    this.callback = callback;
    this.hitCount = 0;

    this.globalEvictionEnabled = globalEvictionEnabled;
    this.globalEvictionFrequency = globalEvictionFrequency;
    initializeGlobalKeySet();
    initializeStore();
  }

  private void initializeGlobalKeySet() {
    for (int i = 0; i < evictorPoolSize; i++) {
      globalKeySet[i] = new GlobalKeySet(cacheName);
    }
  }

  private void initializeStore() {
    for (int i = 0; i < this.concurrency; i++) {
      this.store[i] = new HashMap();
      this.dtmStore[i] = new HashMap();
      ((Clearable) dtmStore[i]).setEvictionEnabled(false);
    }
  }

  public void initialize() {
    this.cacheParticipants = new CacheParticipants();
    this.cacheParticipants.register();

    int numOfStorePerInvalidator = this.concurrency / this.evictorPoolSize;
    int startEvitionIndex = 0;
    this.cacheInvalidationTimer = new CacheInvalidationTimer[evictorPoolSize];
    for (int i = 0; i < evictorPoolSize; i++) {
      int lastEvitionIndex = startEvitionIndex + numOfStorePerInvalidator;
      cacheInvalidationTimer[i] = new CacheInvalidationTimer(invalidatorSleepSeconds, cacheName
                                                                                      + " invalidation thread" + i,
                                                             new CacheEntryInvalidator(globalKeySet[i],
                                                                                       globalEvictionEnabled,
                                                                                       globalEvictionFrequency,
                                                                                       startEvitionIndex,
                                                                                       lastEvitionIndex));
      cacheInvalidationTimer[i].schedule(true);
      startEvitionIndex = lastEvitionIndex;
    }
  }

  /**
   * This is used as a DistributedMethod because when one node cancel a timer, other nodes need to cancel the timer as
   * well.
   */
  public void stopInvalidatorThread() {
    for (int i = 0; i < evictorPoolSize; i++) {
      cacheInvalidationTimer[i].cancel();
    }
  }

  private int getStoreIndex(Object key) {
    if (this.concurrency == 1) { return 0; }
    int hashValue = Math.abs(hash(key.hashCode()));
    return hashValue % this.concurrency;
  }

  public Object put(final Object key, final Object value) {
    Assert.pre(key != null);
    Assert.pre(value != null);

    CacheData cd = new CacheData(value, maxIdleTimeoutSeconds, maxTTLSeconds);
    cd.accessed();
    int storeIndex = getStoreIndex(key);

    CacheData rcd = (CacheData) store[storeIndex].put(key, cd);
    // Only need to put into the timestamp map only when the invalidator thread will be active
    if (invalidatorSleepSeconds >= 0) {
      dtmStore[storeIndex].put(key, cd.getTimestamp());
    }

    Object rv = (rcd == null) ? null : rcd.getValue();
    return rv;
  }

  private void dumpStore() {
    for (int i = 0; i < concurrency; i++) {
      System.err.println("Dump store Client " + ManagerUtil.getClientID() + "i: " + i + " " + store[i]);
    }
  }

  public Object get(final Object key) {
    Assert.pre(key != null);

    CacheData cd = null;
    cd = findCacheDataUnlocked(key);
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
    callback.expire(key);
  }

  public void clear() {
    for (int i = 0; i < this.concurrency; i++) {
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
    for (int i = 0; i < this.concurrency; i++) {
      entrySet.addAll(store[i].entrySet());
    }
    return entrySet;
  }

  public boolean isEmpty() {
    for (int i = 0; i < this.concurrency; i++) {
      if (!store[i].isEmpty()) { return false; }
    }
    return true;
  }

  public Set keySet() {
    Set keySet = new HashSet();
    for (int i = 0; i < this.concurrency; i++) {
      keySet.addAll(store[i].keySet());
    }
    return keySet;
  }

  public boolean containsValue(Object value) {
    CacheData cd = new CacheData(value, this.maxIdleTimeoutSeconds, this.maxTTLSeconds);
    for (int i = 0; i < this.concurrency; i++) {
      if (store[i].containsValue(cd)) { return true; }
    }
    return false;
  }

  public int size() {
    int size = 0;
    for (int i = 0; i < this.concurrency; i++) {
      size += store[i].size();
    }
    return size;
  }

  public Collection values() {
    List values = new ArrayList();
    for (int i = 0; i < this.concurrency; i++) {
      values.addAll(store[i].values());
    }
    return values;
  }

  public long getMaxIdleTimeoutSeconds() {
    return maxIdleTimeoutSeconds;
  }

  public long getMaxTTLSeconds() {
    return maxTTLSeconds;
  }

  void updateTimestampIfNeeded(Object key, CacheData rv) {
    if (maxIdleTimeoutSeconds <= 0) { return; }

    Assert.pre(rv != null);
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    final long expiredTimeMillis = t.getExpiredTimeMillis();
    if (needsUpdate(rv)) {
      int storeIndex = getStoreIndex(key);
      ManagerUtil.monitorEnter(store[storeIndex], LockLevel.CONCURRENT);
      try {
        t.setExpiredTimeMillis(now + rv.getMaxInactiveMillis());
      } finally {
        ManagerUtil.monitorExit(store[storeIndex]);
      }
    }
  }

  boolean needsUpdate(CacheData rv) {
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    final long diff = t.getExpiredTimeMillis() - now;
    return (diff < (rv.getMaxInactiveMillis() / 2) || diff > (rv.getMaxInactiveMillis()));
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

  private Collection getAllLocalEntries(int startEvictionIndex, int lastEvictionIndex) {
    Collection allLocalEntries = new ArrayList();
    for (int i = startEvictionIndex; i < lastEvictionIndex; i++) {
      Collection t = ((TCMap) dtmStore[i]).__tc_getAllLocalEntriesSnapshot();
      allLocalEntries.addAll(t);
    }
    return allLocalEntries;
  }

  private Object[] getAllLocalKeys(int startEvictionIndex, int lastEvictionIndex) {
    Collection allLocalEntires = getAllLocalEntries(startEvictionIndex, lastEvictionIndex);
    Object[] allLocalKeys = new Object[allLocalEntires.size()];
    int i = 0;
    for (Iterator it = allLocalEntires.iterator(); it.hasNext();) {
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
    evictExpiredElements(0, this.concurrency);
  }

  public void evictExpiredElements(int startEvictionIndex, int lastEvictionIndex) {
    final Collection localEntries = getAllLocalEntries(startEvictionIndex, lastEvictionIndex);
    invalidateCacheEntries(localEntries, false, -1, -1);
  }

  public void evictAllExpiredElements(Collection remoteKeys, int startEvictionIndex, int lastEvictionIndex) {
    final Collection orphanEntries = getAllOrphanEntries(remoteKeys, startEvictionIndex, lastEvictionIndex);
    TCProperties ehcacheProperies = ManagerUtil.getTCProperties().getPropertiesFor("ehcache.global.eviction");
    int numOfChunks = ehcacheProperies.getInt("segments");
    long restMillis = ehcacheProperies.getLong("rest.timeMillis");

    invalidateCacheEntries(orphanEntries, true, numOfChunks, restMillis);
  }

  private void invalidateCacheEntries(final Collection entriesToBeExamined, boolean isGlobalInvalidation,
                                      int numOfChunks, long restMillis) {
    int totalCnt = 0;
    int evaled = 0;
    int notEvaled = 0;
    int errors = 0;
    long numOfObjectsPerChunk = entriesToBeExamined.size();

    if (isGlobalInvalidation) {
      numOfObjectsPerChunk = Math.round(entriesToBeExamined.size() * 1.0 / numOfChunks);
    }

    for (Iterator it = entriesToBeExamined.iterator(); it.hasNext();) {
      final Map.Entry timestampEntry = (Map.Entry) it.next();

      try {
        final Timestamp dtm = findTimestampUnlocked(timestampEntry.getKey());
        if (dtm == null) continue;
        totalCnt++;
        if (dtm.getInvalidatedTimeMillis() < System.currentTimeMillis()) {
          evaled++;
          expire(timestampEntry.getKey());
        } else {
          notEvaled++;
        }
      } catch (Throwable t) {
        errors++;
        t.printStackTrace(System.err);
        // logger.error("Unhandled exception inspecting session " + key + " for invalidation", t);
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

  private class CacheEntryInvalidator implements Runnable {
    private final Lock         globalInvalidationLock;
    private final Lock         localInvalidationLock;
    private boolean            isGlobalInvalidator = false;
    private final GlobalKeySet globalKeySet;
    private final boolean      globalEvictionEnabled;
    private final int          globalEvictionFrequency;
    private final int          startEvictionIndex;
    private final int          lastEvictionIndex;
    private int                numOfLocalEvictionOccurred;

    public CacheEntryInvalidator(GlobalKeySet globalKeySet, boolean globalEvictionEnabled, int globalEvictionFrequency,
                                 int startEvitionIndex, int lastEvitionIndex) {
      String localEvictionLockName = "tc:local_time_expiry_cache_invalidator_lock_" + cacheName + ":"
                                     + startEvitionIndex + ":" + lastEvitionIndex;
      String globalEvictionLockName = "tc:global_time_expiry_cache_invalidator_lock_" + cacheName + ":"
                                      + startEvitionIndex + ":" + lastEvitionIndex;
      this.globalKeySet = globalKeySet;
      this.localInvalidationLock = new Lock(localEvictionLockName);
      this.globalInvalidationLock = new Lock(globalEvictionLockName);
      this.globalEvictionEnabled = globalEvictionEnabled;
      this.globalEvictionFrequency = globalEvictionFrequency;
      this.startEvictionIndex = startEvitionIndex;
      this.lastEvictionIndex = lastEvitionIndex;
    }

    public void run() {
      try {
        tryToBeGlobalInvalidator();

        localInvalidationLock.writeLock();
        try {
          evictLocalElements();
        } finally {
          localInvalidationLock.commitLock();
          numOfLocalEvictionOccurred++;
        }
        globalEvictionIfNecessary();
      } catch (Throwable t) {
        t.printStackTrace(System.err);
        throw new TCRuntimeException(t);
      }
    }

    protected void evictLocalElements() {
      evictExpiredElements(this.startEvictionIndex, this.lastEvictionIndex);
    }

    private void globalEvictionIfNecessary() {
      if (!isGlobalEvictionEnabled()) { return; }

      if (isTimeForGlobalInvalidation()) {
        try {
          if (isGlobalInvalidator) {
            try {
              globalEvictionStarted();
              evictAllExpiredElements(globalKeySet.allGlobalKeys(), startEvictionIndex, lastEvictionIndex);
            } finally {
              globalKeySet.globalEvictionEnd();
            }
          } else {
            waitForGlobalEviction();
            notifyLocalKeySet();
          }
        } finally {
          numOfLocalEvictionOccurred = 0;
        }
      }
    }

    private void waitForGlobalEviction() {
      globalKeySet.waitForGlobalEviction();
    }

    private void notifyLocalKeySet() {
      globalKeySet
          .addLocalKeySet(cacheParticipants.getNodeId(), getAllLocalKeys(startEvictionIndex, lastEvictionIndex));
    }

    private void globalEvictionStarted() {
      globalKeySet.globalEvictionStart(cacheParticipants.getNodeId(), cacheParticipants.getCacheParticipants());
    }

    public void postRun() {
      if (isGlobalInvalidator) {
        globalInvalidationLock.commitLock();
        isGlobalInvalidator = false;
      }
    }

    private boolean isTimeForGlobalInvalidation() {
      Assert.eval(globalEvictionFrequency > 0);
      return globalEvictionFrequency == numOfLocalEvictionOccurred;
    }

    private boolean isGlobalEvictionEnabled() {
      return globalEvictionEnabled;
    }

    private void tryToBeGlobalInvalidator() {
      if (isGlobalEvictionEnabled() && !isGlobalInvalidator) {
        if (globalInvalidationLock.tryWriteLock()) {
          isGlobalInvalidator = true;
        }
      }
    }

  }

  private class CacheInvalidationTimer implements Runnable {
    private final long                            delayMillis;
    private boolean                               recurring = true;
    private final String                          timerName;
    private final transient CacheEntryInvalidator invalidationTask;

    public CacheInvalidationTimer(final long delayInSecs, final String timerName,
                                  final CacheEntryInvalidator invalidationTask) {
      this.timerName = timerName;
      this.invalidationTask = invalidationTask;
      this.delayMillis = delayInSecs * 1000;
    }

    public void schedule(boolean recurring) {
      if (delayMillis <= 0) { return; }

      this.recurring = recurring;
      Thread t = new Thread(this, timerName);
      t.setDaemon(true);
      t.start();
    }

    public void run() {
      long nextDelay = delayMillis;
      try {
        do {
          sleep(nextDelay);
          invalidationTask.run();
        } while (recurring);
      } finally {
        invalidationTask.postRun();
      }
    }

    private void sleep(long l) {
      try {
        Thread.sleep(l);
      } catch (InterruptedException ignore) {
        // nothing to do
      }
    }

    public void cancel() {
      recurring = false;
    }
  }
}
