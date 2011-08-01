/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;

import java.util.Map;

public class L1ServerMapCapacityEvictionHandler extends AbstractEventHandler {
  private volatile L1ServerMapLocalCacheManager globalLocalCacheManager;

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof L1ServerMapLocalStoreEvictionInfo) {
      doCapacityEviction((L1ServerMapLocalStoreEvictionInfo) context);
    } else {
      evictElements((L1ServerMapEvictedElementsContext) context);
    }
  }

  private void evictElements(L1ServerMapEvictedElementsContext context) {
    Map evictedElements = context.getEvictedElements();
    globalLocalCacheManager.evictElements(evictedElements);
  }

  private void doCapacityEviction(L1ServerMapLocalStoreEvictionInfo evictionInfo) {
    try {
      L1ServerMapLocalCacheStore store = evictionInfo.getL1ServerMapLocalCacheStore();
      final int maxElementsInMemory = store.getMaxElementsInMemory();
      if (maxElementsInMemory == 0) {
        // 0 means disabled
        return;
      }
      int overshoot = store.size() - maxElementsInMemory;
      if (overshoot <= 0) { return; }

      int elementsToEvict = (int) ((maxElementsInMemory * 20.0) / 100 + overshoot);
      store.evict(elementsToEvict);
    } finally {
      evictionInfo.markEvictionComplete();
    }
  }

  public void initialize(L1ServerMapLocalCacheManager localCacheManager) {
    this.globalLocalCacheManager = localCacheManager;
  }
}
