/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.cache;

public interface Evictable {

  public void evictCache(CacheStats stat);
}
