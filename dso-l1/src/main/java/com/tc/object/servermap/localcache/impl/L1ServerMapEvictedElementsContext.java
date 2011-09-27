/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

import java.util.Map;

public class L1ServerMapEvictedElementsContext implements EventContext {
  private final Map                 evictedElements;
  private final ServerMapLocalCache serverMapLocalCache;

  public L1ServerMapEvictedElementsContext(Map evictedElements, ServerMapLocalCache serverMapLocalCache) {
    this.evictedElements = evictedElements;
    this.serverMapLocalCache = serverMapLocalCache;
  }

  public Map getEvictedElements() {
    return evictedElements;
  }

  public ServerMapLocalCache getServerMapLocalCache() {
    return serverMapLocalCache;
  }
}
