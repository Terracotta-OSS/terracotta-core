/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class L1ServerMapEvictedElementsContext implements EventContext {
  private static final TCLogger      logger              = TCLogging.getLogger(L1ServerMapEvictedElementsContext.class);
  private static final AtomicInteger elementsToBeEvicted = new AtomicInteger();
  private static final AtomicInteger instanceCount       = new AtomicInteger();

  private final Map                  evictedElements;
  private final ServerMapLocalCache  serverMapLocalCache;

  public L1ServerMapEvictedElementsContext(Map evictedElements, ServerMapLocalCache serverMapLocalCache) {
    this.evictedElements = evictedElements;
    this.serverMapLocalCache = serverMapLocalCache;
    int currentInstanceCount = instanceCount.incrementAndGet();
    if (elementsToBeEvicted.addAndGet(evictedElements.size()) > 200 && currentInstanceCount % 10 == 0
        && logger.isDebugEnabled()) {
      logger.debug("Elements waiting to be evicted: " + elementsToBeEvicted.get());
    }
  }

  public Map getEvictedElements() {
    return evictedElements;
  }

  public ServerMapLocalCache getServerMapLocalCache() {
    return serverMapLocalCache;
  }

  public void elementsEvicted() {
    elementsToBeEvicted.addAndGet(-evictedElements.size());
  }
}
