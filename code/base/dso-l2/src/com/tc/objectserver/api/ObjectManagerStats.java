/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

public interface ObjectManagerStats {

  double getCacheHitRatio();

  long getTotalRequests();

  long getTotalCacheMisses();

  long getTotalCacheHits();
  
  long getTotalObjectsCreated();

}
