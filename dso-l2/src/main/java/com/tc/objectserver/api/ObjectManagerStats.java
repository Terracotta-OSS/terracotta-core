/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.stats.counter.sampled.TimeStampedCounterValue;

public interface ObjectManagerStats {

  double getCacheHitRatio();
  
  TimeStampedCounterValue getOnHeapFaultRate();
 
  TimeStampedCounterValue getOnHeapFlushRate();
  
  long getTotalRequests();

  long getTotalCacheMisses();

  long getTotalCacheHits();
  
  long getTotalObjectsCreated();

}
