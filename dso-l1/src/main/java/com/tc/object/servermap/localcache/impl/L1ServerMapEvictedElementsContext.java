/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;

import java.util.Map;

public class L1ServerMapEvictedElementsContext implements EventContext {
  private final Map evictedElements;

  public L1ServerMapEvictedElementsContext(Map evictedElements, L1ServerMapLocalCacheManager globalLocalCacheManager) {
    this.evictedElements = evictedElements;
  }

  public Map getEvictedElements() {
    return evictedElements;
  }
}
