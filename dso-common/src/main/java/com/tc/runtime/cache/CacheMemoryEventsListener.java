/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime.cache;

import com.tc.runtime.MemoryUsage;

public interface CacheMemoryEventsListener {
  
  public void memoryUsed(CacheMemoryEventType type, MemoryUsage usage);
  
}
