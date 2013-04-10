/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;

public interface DSOGlobalServerStats {

  SampledCounter getReadOperationRateCounter();

  SampledCounter getTransactionCounter();

  SampledCounter getEvictionRateCounter();

  SampledCounter getExpirationRateCounter();

  ObjectManagerStats getObjectManagerStats();

  SampledCounter getBroadcastCounter();

  SampledCounter getGlobalLockRecallCounter();

  SampledRateCounter getChangesPerBroadcastCounter();

  SampledRateCounter getTransactionSizeCounter();

  SampledCounter getGlobalLockCounter();
  
  SampledCumulativeCounter getServerMapGetSizeRequestsCounter();

  SampledCumulativeCounter getServerMapGetValueRequestsCounter();

  SampledCounter getOperationCounter();
}
