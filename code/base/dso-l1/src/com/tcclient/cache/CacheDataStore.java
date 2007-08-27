/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.config.lock.LockLevel;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class CacheDataStore implements Serializable {
  private final Map                       store;                  // <Data>
  private final Map                       dtmStore;               // <Timestamp>
  private final String                    cacheName;
  private final long                      maxIdleTimeoutSeconds;
  private final long                      maxTTLSeconds;
  private final long                      invalidatorSleepSeconds;
  private Expirable                       callback;
  private transient int                   hitCount;
  private transient int                   missCountExpired;
  private transient int                   missCountNotFound;
  private transient CacheEntryInvalidator cacheInvalidator;

  public CacheDataStore(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, long maxTTLSeconds, Map store,
                        Map dtmStore, String cacheName, Expirable callback) {
    this.store = store;
    this.dtmStore = dtmStore;
    this.cacheName = cacheName;
    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;
    this.maxTTLSeconds = maxTTLSeconds;
    this.invalidatorSleepSeconds = invalidatorSleepSeconds;
    this.callback = callback;

    this.hitCount = 0;
  }

  public CacheDataStore(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, Map store, Map dtmStore,
                        String cacheName, Expirable callback) {
    this(invalidatorSleepSeconds, maxIdleTimeoutSeconds, Long.MAX_VALUE, store, dtmStore, cacheName, callback);
  }

  public void initialize() {
    cacheInvalidator = new CacheEntryInvalidator(invalidatorSleepSeconds);
    cacheInvalidator.scheduleNextInvalidation();
  }

  public void stopInvalidatorThread() {
    cacheInvalidator.cancel();
  }

  public Object put(final Object key, final Object value) {
    Assert.pre(key != null);
    Assert.pre(value != null);
    CacheData cd = (CacheData) store.get(key);
    Object rv = (cd == null) ? null : cd.getValue();

    cd = new CacheData(value, maxIdleTimeoutSeconds, maxTTLSeconds);
    ManagerUtil.monitorEnter(store, LockLevel.WRITE);
    try {
      cd.accessed();
      if (DebugUtil.DEBUG) {
        System.err.println("Client " + ManagerUtil.getClientID() + " putting " + key);
      }
      store.put(key, cd);
      dtmStore.put(key, cd.getTimestamp());
    } finally {
      ManagerUtil.monitorExit(store);
    }
    return rv;
  }

  public Object get(final Object key) {
    Assert.pre(key != null);

    CacheData cd = null;
    cd = (CacheData) store.get(key);
    if (cd != null) {
      if (!cd.isValid()) {
        missCountExpired++;
        invalidate(cd);
        if (DebugUtil.DEBUG) {
          System.err.println("Client " + ManagerUtil.getClientID() + " rv is not valid -- key: " + key + ", value: " + cd.getValue() + " rv.getIdleMillis(): "
                             + cd.getIdleMillis());
        }
        return null;
      } else {
        hitCount++;
        cd.accessed();
        updateTimestampIfNeeded(cd);
      }
      return cd.getValue();
    }
    missCountNotFound++;
    return null;
  }

  private void invalidate(CacheData cd) {
    if (!cd.isInvalidated()) {
      ManagerUtil.monitorEnter(store, LockLevel.WRITE);
      try {
        cd.invalidate();
      } finally {
        ManagerUtil.monitorExit(store);
      }
    }
  }

  public boolean isExpired(final Object key) {
    CacheData rv = (CacheData) store.get(key);
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " checking isExpired for key: " + key + " rv: " + rv
                         + " rv.isValid: " + ((rv == null) ? false : rv.isValid()));
    }
    return rv == null || !rv.isValid();
  }

  public Object remove(final Object key) {
    CacheData cd = (CacheData) store.get(key);
    if (cd == null) return null;
    removeInternal(key);
    return cd.getValue();
  }

  private void removeInternal(final Object key) {
    Assert.pre(key != null);

    ManagerUtil.monitorEnter(store, LockLevel.WRITE);
    try {
      store.remove(key);
      dtmStore.remove(key);
    } finally {
      ManagerUtil.monitorExit(store);
    }
  }

  public void expire(Object key) {
    removeInternal(key);
    callback.expire(key);
  }

  public void clear() {
    store.clear();
    dtmStore.clear();
  }

  public Map getStore() {
    return store;
  }

  public long getMaxIdleTimeoutSeconds() {
    return maxIdleTimeoutSeconds;
  }

  public long getMaxTTLSeconds() {
    return maxTTLSeconds;
  }

  void updateTimestampIfNeeded(CacheData rv) {
    if (maxIdleTimeoutSeconds <= 0) { return; }

    Assert.pre(rv != null);
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    final long expiredTimeMillis = t.getExpiredTimeMillis();
    if (needsUpdate(rv)) {
      ManagerUtil.monitorEnter(store, LockLevel.WRITE);
      try {
        if (DebugUtil.DEBUG) {
          System.err.println("Client " + ManagerUtil.getClientID() + " expiredTimeMillis before monitorEnter: "
                             + expiredTimeMillis + " expiredTimeMillis after monitorEnter: " + t.getExpiredTimeMillis()
                             + " setting ExpiredTimeMillis: " + (now + rv.getMaxInactiveMillis()));
        }
        t.setExpiredTimeMillis(now + rv.getMaxInactiveMillis());
      } finally {
        ManagerUtil.monitorExit(store);
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
    return (Timestamp) dtmStore.get(key);
  }

  CacheData findCacheDataUnlocked(final Object key) {
    final CacheData rv = (CacheData) store.get(key);
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

  private Object[] getAllKeys() {
    Object[] rv;
    ManagerUtil.monitorEnter(store, LockLevel.WRITE);
    try {
      synchronized (store) {
        Set keys = dtmStore.keySet();
        rv = keys.toArray(new Object[keys.size()]);
      }
    } finally {
      ManagerUtil.monitorExit(store);
    }
    Assert.post(rv != null);
    return rv;
  }

  public void evictExpiredElements() {
    final String invalidatorLock = "tc:time_expiry_cache_invalidator_lock_" + cacheName;

    final Lock lock = new Lock(invalidatorLock);
    lock.tryWriteLock();
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " tryWriteLock status: " + lock.isLocked() + " lockName: " + invalidatorLock);
    }
    if (!lock.isLocked()) return;

    try {
      invalidateCacheEntries();
    } finally {
      lock.commitLock();
    }
  }

  private void invalidateCacheEntries() {
    final Object[] keys = getAllKeys();
    int totalCnt = 0;
    int invalCnt = 0;
    int evaled = 0;
    int notEvaled = 0;
    int errors = 0;

    for (int i = 0; i < keys.length; i++) {
      final Object key = keys[i];
      try {
        final Timestamp dtm = findTimestampUnlocked(key);
        if (dtm == null) continue;
        totalCnt++;
        if (DebugUtil.DEBUG) {
          System.err.println("Client id: " + ManagerUtil.getClientID() + " key: " + key
                             + " InvalidateCacheEntries [dtm.getMillis]: " + dtm.getInvalidatedTimeMillis()
                             + " [currentMillis]: " + System.currentTimeMillis());
        }
        if (dtm.getInvalidatedTimeMillis() < System.currentTimeMillis()) {
          evaled++;
          expire(key);
          //if (evaluateCacheEntry(dtm, key)) invalCnt++;
        } else {
          notEvaled++;
        }
      } catch (Throwable t) {
        errors++;
        t.printStackTrace(System.err);
        // logger.error("Unhandled exception inspecting session " + key + " for invalidation", t);
      }
    }
  }

//  private boolean evaluateCacheEntry(final Timestamp dtm, final Object key) {
//    Assert.pre(key != null);
//
//    boolean rv = false;
//
//    final CacheData sd = findCacheDataUnlocked(key);
//    if (sd == null) return rv;
//    if (!sd.isValid()) {
//      if (DebugUtil.DEBUG) {
//        System.err.println("Client " + ManagerUtil.getClientID() + " expiring " + key);
//      }
//      expire(key, sd);
//      rv = true;
//      // } else {
//      // updateTimestampIfNeeded(sd);
//    }
//    return rv;
//  }

  private class CacheEntryInvalidator extends TimerTask {
    private final long  sleepMillis;
    private final Timer timer;

    public CacheEntryInvalidator(final long sleepSeconds) {
      this.sleepMillis = sleepSeconds * 1000;
      this.timer = new Timer(true);
    }

    public void scheduleNextInvalidation() {
      if (DebugUtil.DEBUG) {
        System.err.println("Client " + ManagerUtil.getClientID() + " schedule next invalidation " + this.sleepMillis);
      }
      // If sleepMillis is <= 0, there will be no eviction, honoring the native ehcache semantics.
      if (this.sleepMillis <= 0) { return; }
      timer.schedule(this, this.sleepMillis, this.sleepMillis);
    }

    public void run() {
      try {
        if (DebugUtil.DEBUG) {
          System.err.println("Client " + ManagerUtil.getClientID() + " running evictExpiredElements");
        }
        evictExpiredElements();
      } catch (Throwable t) {
        t.printStackTrace(System.err);
        throw new TCRuntimeException(t);
      }
    }
  }
}
