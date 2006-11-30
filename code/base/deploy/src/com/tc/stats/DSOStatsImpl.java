/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats;

import java.lang.reflect.Method;

import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.statistics.DoubleStatistic;
import com.tc.stats.statistics.DoubleStatisticImpl;

import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.CountStatistic;

/**
 * This is the root interface to the global DSO Server statistics.
 * 
 * @see StatsSupport
 * @see DSOStats
 */

public class DSOStatsImpl extends StatsSupport implements DSOStats {

  private final DSOGlobalServerStats serverStats;
  private final SampledCounter       faultRate;
  private final SampledCounter       flushRate;
  private final ObjectManagerStats   objMgrStats;
  private final SampledCounter       txnRate;
  private final ObjectManagerMBean   objManager;

  public DSOStatsImpl(ServerManagementContext context) {
    this.objManager = context.getObjectManager();
    this.serverStats = context.getServerStats();
    this.objMgrStats = serverStats.getObjectManagerStats();
    this.faultRate = serverStats.getObjectFaultCounter();
    this.flushRate = serverStats.getObjectFlushCounter();
    this.txnRate = serverStats.getTransactionCounter();
  }

  public CountStatistic getObjectFaultRate() {
    return StatsUtil.makeCountStat(faultRate, "Object Fault Rate", "objects faulted per second",
                                   "DSO total objects faulted per second");
  }

  public CountStatistic getObjectFlushRate() {
    return StatsUtil.makeCountStat(flushRate, "Object Flush Rate", "objects flushed per second",
                                   "DSO total objects flushed per second");
  }

  public CountStatistic getTransactionRate() {
    return StatsUtil.makeCountStat(txnRate, "Transaction Rate", "transactions per second",
                                   "DSP total transactions per second");
  }

  public DoubleStatistic getCacheHitRatio() {
    double value = objMgrStats.getCacheHitRatio();
    DoubleStatisticImpl rv = new DoubleStatisticImpl("Server Cache Hit Ratio", "hits/total",
                                                     "The ratio of cache hits over total cache requests", 0L, System
                                                         .currentTimeMillis());
    rv.setDoubleValue(value);
    return rv;
  }

  public Statistic[] getStatistics(String[] names) {
    int         count  = names.length;
    Statistic[] result = new Statistic[count];
    Method      method;

    for(int i = 0; i < count; i++) {
      try {
        method = getClass().getMethod("get"+names[i], new Class[]{});
        result[i] = (Statistic)method.invoke(this, new Object[]{});
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }

    return result;
  }

  public GCStats[] getGarbageCollectorStats() {
    return this.objManager.getGarbageCollectorStats();
  }

}
