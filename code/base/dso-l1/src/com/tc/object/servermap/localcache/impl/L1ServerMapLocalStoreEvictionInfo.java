/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;

import java.util.concurrent.atomic.AtomicBoolean;

public class L1ServerMapLocalStoreEvictionInfo implements EventContext {
  private final AtomicBoolean              evictionInProgress = new AtomicBoolean(false);
  private final L1ServerMapLocalCacheStore store;

  public L1ServerMapLocalStoreEvictionInfo(L1ServerMapLocalCacheStore store) {
    this.store = store;
  }

  public L1ServerMapLocalCacheStore getL1ServerMapLocalCacheStore() {
    return store;
  }

  public boolean attemptEvictionStart() {
    int maxElementsInMemory = store.getMaxElementsInMemory();
    if (maxElementsInMemory == 0 || evictionInProgress.get() || store.size() < maxElementsInMemory) {
      // no capacity eviction required when:
      // 1: disabled (maxElementsInMemory=0)
      // 2: already in progress
      // 3: no overshoot
      return false;
    }
    return evictionInProgress.compareAndSet(false, true);
  }

  public void markEvictionComplete() {
    evictionInProgress.set(false);
  }
}