/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
