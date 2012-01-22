/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import java.util.List;

public interface CacheStats {
  
  public int getObjectCountToEvict(int currentCount);
  
  public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC, boolean printNewObjects);
}
