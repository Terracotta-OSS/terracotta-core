/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import java.util.List;

public interface CacheStats {
  
  public int getObjectCountToEvict(int currentCount);
  
  public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC, boolean printNewObjects);
}
