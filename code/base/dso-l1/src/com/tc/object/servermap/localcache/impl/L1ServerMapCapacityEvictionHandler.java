/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;

import java.util.Map;

public class L1ServerMapCapacityEvictionHandler extends AbstractEventHandler {
  private volatile L1ServerMapLocalCacheManager globalLocalCacheManager;

  @Override
  public void handleEvent(EventContext context) {
    evictElements((L1ServerMapEvictedElementsContext) context);
  }

  private void evictElements(L1ServerMapEvictedElementsContext context) {
    Map evictedElements = context.getEvictedElements();
    globalLocalCacheManager.evictElements(evictedElements);
  }

  public void initialize(L1ServerMapLocalCacheManager localCacheManager) {
    this.globalLocalCacheManager = localCacheManager;
  }
}
