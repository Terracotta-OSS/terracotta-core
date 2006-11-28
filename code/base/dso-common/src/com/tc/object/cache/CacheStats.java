/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.cache;

import java.util.List;

public interface CacheStats {
  
  public int getObjectCountToEvict(int currentCount);
  
  public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC);
}
