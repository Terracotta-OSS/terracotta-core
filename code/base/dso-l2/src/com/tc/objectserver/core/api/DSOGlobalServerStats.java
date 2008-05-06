/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.stats.counter.sampled.SampledCounter;

public interface DSOGlobalServerStats {

  SampledCounter getObjectFlushCounter();

  SampledCounter getObjectFaultCounter();
  
  SampledCounter getTransactionCounter();
  
  ObjectManagerStats getObjectManagerStats();

  SampledCounter getBroadcastCounter();

  SampledCounter getChangesCounter();

}
