/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;

import java.util.Map;

public class L1ServerMapCapacityEvictionHandler extends AbstractEventHandler {
  private volatile L1ServerMapLocalCacheManager l1LocalCacheManager;

  @Override
  public void handleEvent(EventContext context) {
    L1ServerMapEvictedElementsContext evictedElementsContext = (L1ServerMapEvictedElementsContext) context;
    Map evictedElements = evictedElementsContext.getEvictedElements();
    l1LocalCacheManager.evictElements(evictedElements, evictedElementsContext.getServerMapLocalCache());
    evictedElementsContext.elementsEvicted();
  }

  public void initialize(L1ServerMapLocalCacheManager localCacheManager) {
    this.l1LocalCacheManager = localCacheManager;
  }
}
