/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.object.bytecode.Manager;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

import java.util.Collection;
import java.util.Date;

/**
 * The invalidator watches a portion of the CacheDataStore (a range of the submaps) and is run in a loop, periodically
 * running an eviction of all keys existent in this node. Remote keys are ignored to minimize faulting over the network.
 * Additionally, at the beginning of each run, the cache attempts to become the "global evictor". For each chunk of the
 * cache, only one of the local evictors in the cluster will become the global evictor. That thread is responsible for
 * both its own local eviction and also the global eviction. Global eviction entails checking "orphan" keys (those not
 * currently faulted into any node's cache) for eviction. The process of doing this will cause those keys to be loaded
 * into this node such that they are no longer orphans. This class should be completely local and no state should be
 * shared across the cluster. This is done by indicating transient on the array holding these objects in the
 * CacheDataStore.
 */
class CacheEntryInvalidator {

  // Configuration
  private final CacheConfig    config;
  private final int            startEvictionIndex;
  private final int            lastEvictionIndex;
  private final int            localCyclesToWaitDuringGlobal = 2; // hard-coded, could be configurable if needed

  // Resources
  private final TCLogger       logger;
  private final CacheDataStore store;
  private final GlobalKeySet   globalKeySet;

  // Local eviction state
  private final Lock           localInvalidationLock;

  // Global eviction state
  private boolean              isGlobalInvalidator;
  private final Lock           globalInvalidationLock;
  private int                  evictionCount;                    // when this == globalEvictionFrequency, do global
                                                                  // eviction
  private int                  globalWaitCount;                  // number of local eviction cycles we've waited during
                                                                  // global eviction

  public CacheEntryInvalidator(GlobalKeySet globalKeySet, int startEvictionIndex, int lastEvictionIndex,
                               CacheConfig config, Manager manager, CacheDataStore store) {

    // Copy configuration
    this.config = config;
    if (config.isGlobalEvictionEnabled()) {
      Assert.eval(config.getGlobalEvictionFrequency() > 0);
    }
    this.startEvictionIndex = startEvictionIndex;
    this.lastEvictionIndex = lastEvictionIndex;

    // Copy resources
    this.globalKeySet = globalKeySet;
    this.store = store;

    // Initialize state
    logger = manager.getLogger("com.tc.cache.CacheEntryInvalidator");
    this.localInvalidationLock = new Lock(createLockName("tc:local_time_expiry_cache_invalidator_lock_", config
        .getCacheName(), startEvictionIndex), manager);
    this.globalInvalidationLock = new Lock(createLockName("tc:global_time_expiry_cache_invalidator_lock_", config
        .getCacheName(), startEvictionIndex), manager);
  }

  private String createLockName(String prefix, String cacheName, int startIndex) {
    return prefix + cacheName + ":" + startIndex;
  }

  /**
   * Runnable run method
   */
  public void run() {
    try {
      tryToBeGlobalInvalidator();

      // Do local eviction
      for (int i = startEvictionIndex; i < lastEvictionIndex; i++) {
        localInvalidationLock.writeLock();
        try {
          evictLocalElements(i);
        } finally {
          localInvalidationLock.commitLock();
        }
      }

      // Do global eviction if global evictor
      globalEvictionIfNecessary();

    } catch (Throwable t) {
      t.printStackTrace(System.err);
      throw new TCRuntimeException(t);
    }
  }

  /**
   * Perform local eviction on this invalidator's portion of the store
   */
  protected void evictLocalElements(int index) {
    log("Local eviction started");
    store.evictExpiredElements(index, index+1);
    log("Local eviction finished");
  }

  /**
   * Attempt to obtain global invalidator write lock. If successful, set isGlobalInvalidator to true
   */
  private void tryToBeGlobalInvalidator() {
    if (config.isGlobalEvictionEnabled() && !isGlobalInvalidator) {
      if (globalInvalidationLock.tryWriteLock()) {
        isGlobalInvalidator = true;
      }
    }
  }

  /**
   * Perform global eviction
   */
  private void globalEvictionIfNecessary() {
    if (!config.isGlobalEvictionEnabled()) { return; }

    evictionCount++;
    boolean isTimeForGlobalEviction = evictionCount >= config.getGlobalEvictionFrequency();
    if (isTimeForGlobalEviction) {
      evictionCount = 0;
    }

    boolean inGlobalEviction = globalKeySet.inGlobalEviction();

    if (isGlobalInvalidator) {
      if (!inGlobalEviction && isTimeForGlobalEviction) {
        // Start global eviction
        log("Global eviction started");
        globalKeySet.globalEvictionStart(getLocalKeys());
        globalWaitCount = 0;

      } else if (inGlobalEviction) {
        globalWaitCount++;
        if (globalWaitCount >= localCyclesToWaitDuringGlobal) {
          // End global eviction
          Collection remoteKeys = globalKeySet.globalEvictionEnd();
          store.evictAllExpiredElements(remoteKeys, startEvictionIndex, lastEvictionIndex);
          log("Global eviction finished");
        }
      }
    } else if (inGlobalEviction) {
      // Non-global evictor participating in global eviction
      globalKeySet.addLocalKeySet(getLocalKeys());
    }
  }

  private Object[] getLocalKeys() {
    return store.getAllLocalKeys(startEvictionIndex, lastEvictionIndex);
  }

  public void postRun() {
    if (isGlobalInvalidator) {
      globalInvalidationLock.commitLock();
      isGlobalInvalidator = false;
    }
  }

  private void log(String msg) {
    if (config.isEvictorLoggingEnabled()) {
      logger.debug(msg);
      if (DebugUtil.DEBUG) {
        System.err.println((new Date(System.currentTimeMillis())).toString() + msg);
      }
    }
  }

}
