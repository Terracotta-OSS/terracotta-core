/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounterConfig;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import junit.framework.TestCase;

public class SRAL2ServerMapGetValueRequestsRateTest extends TestCase {

  private DSOGlobalServerStatsImpl dsoGlobalServerStats;
  private CounterIncrementer       counterIncrementer;

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCumulativeCounterConfig sampledCounterConfig = new SampledCumulativeCounterConfig(1, 10, true, 0L);
    final SampledCumulativeCounter getValueCounter = (SampledCumulativeCounter) counterManager
        .createCounter(sampledCounterConfig);

    dsoGlobalServerStats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null, null, null,
                                                        null, null);
    dsoGlobalServerStats.serverMapGetValueRequestsCounter(getValueCounter);

    counterIncrementer = new CounterIncrementer(getValueCounter, 200);
    new Thread(counterIncrementer, "Counter Incrementer").start();
  }

  public void testRetrieval() {
    SRAL2ServerMapGetValueRequestsRate sral2TransactionCount = new SRAL2ServerMapGetValueRequestsRate(
                                                                                                      dsoGlobalServerStats);
    Assert.assertEquals(StatisticType.SNAPSHOT, sral2TransactionCount.getType());

    StatisticData[] statisticDatas;

    statisticDatas = sral2TransactionCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2ServerMapGetValueRequestsRate.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count1 = (Long) statisticDatas[0].getData();
    Assert.eval(count1 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = sral2TransactionCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2ServerMapGetValueRequestsRate.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count2 = (Long) statisticDatas[0].getData();
    Assert.eval(count2 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = sral2TransactionCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2ServerMapGetValueRequestsRate.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count3 = (Long) statisticDatas[0].getData();
    Assert.eval(count3 >= 0);
  }

  protected void tearDown() throws Exception {
    counterIncrementer.stopCounterIncrement();
    counterIncrementer = null;
    dsoGlobalServerStats = null;
  }
}
