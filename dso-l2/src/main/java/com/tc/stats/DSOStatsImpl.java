/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.stats.api.DSOStats;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;

import java.lang.reflect.Method;

/**
 * This is the root interface to the global DSO Server statistics.
 * 
 * @see StatsSupport
 * @see DSOStats
 */

public class DSOStatsImpl implements DSOStats {

  private final DSOGlobalServerStats serverStats;
  private final SampledCounter       faultRate;
  private final SampledCounter       txnRate;
  private final SampledCounter       globalLockRecallRate;
  private final SampledRateCounter   transactionSizeRate;
  private final SampledCounter       broadcastRate;

  public DSOStatsImpl(ServerManagementContext context) {
    this.serverStats = context.getServerStats();
    this.faultRate = serverStats.getReadOperationRateCounter();
    this.txnRate = serverStats.getTransactionCounter();
    this.globalLockRecallRate = serverStats.getGlobalLockRecallCounter();
    this.transactionSizeRate = serverStats.getTransactionSizeCounter();
    this.broadcastRate = serverStats.getBroadcastCounter();
  }

  @Override
  public long getReadOperationRate() {
    return faultRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getTransactionRate() {
    return txnRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getEvictionRate() {
    return serverStats.getEvictionRateCounter().getMostRecentSample().getCounterValue();
  }

  @Override
  public long getExpirationRate() {
    return serverStats.getExpirationRateCounter().getMostRecentSample().getCounterValue();
  }

  @Override
  public long getGlobalLockRecallRate() {
    return globalLockRecallRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getTransactionSizeRate() {
    return transactionSizeRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getBroadcastRate() {
    return broadcastRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public Number[] getStatistics(String[] names) {
    int count = names.length;
    Number[] result = new Number[count];
    Method method;

    for (int i = 0; i < count; i++) {
      try {
        method = getClass().getMethod("get" + names[i], new Class[] {});
        result[i] = (Number) method.invoke(this, new Object[] {});
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return result;
  }

  @Override
  public long getGlobalServerMapGetSizeRequestsCount() {
    return serverStats.getServerMapGetSizeRequestsCounter().getCumulativeValue();
  }

  @Override
  public long getGlobalServerMapGetSizeRequestsRate() {
    return serverStats.getServerMapGetSizeRequestsCounter().getMostRecentSample().getCounterValue();
  }

  @Override
  public long getGlobalServerMapGetValueRequestsCount() {
    return serverStats.getServerMapGetValueRequestsCounter().getCumulativeValue();
  }

  @Override
  public long getGlobalServerMapGetValueRequestsRate() {
    return serverStats.getServerMapGetValueRequestsCounter().getMostRecentSample().getCounterValue();
  }

  @Override
  public long getWriteOperationRate() {
    return serverStats.getOperationCounter().getMostRecentSample().getCounterValue();
  }
}
