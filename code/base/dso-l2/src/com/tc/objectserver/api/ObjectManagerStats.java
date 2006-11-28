/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

public interface ObjectManagerStats {

  double getCacheHitRatio();

  long getTotalRequests();

  long getTotalCacheMisses();

  long getTotalCacheHits();
  
  long getTotalObjectsCreated();

}
