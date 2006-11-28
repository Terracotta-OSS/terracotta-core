/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.stats.counter.sampled.SampledCounter;

public interface DSOGlobalServerStats {

  SampledCounter getObjectFlushCounter();

  SampledCounter getObjectFaultCounter();
  
  SampledCounter getTransactionCounter();
  
  ObjectManagerStats getObjectManagerStats();

}
