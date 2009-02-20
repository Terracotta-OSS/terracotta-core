/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import junit.framework.TestCase;

import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

public class SRAL1ToL2FlushRateTest extends TestCase {
  private DSOGlobalServerStats dsoGlobalServerStats;
  private CounterIncrementer   counterIncrementer;

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 10, true, 0L);
    final SampledCounter l1Tol2FlushCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);

    dsoGlobalServerStats = new DSOGlobalServerStatsImpl(l1Tol2FlushCounter, null, null, null, null, null, null, null,
                                                        null, null, null, null);

    counterIncrementer = new CounterIncrementer(l1Tol2FlushCounter, 200);
    new Thread(counterIncrementer, "Counter Incrementer").start();
  }

  public void testRetrieval() {
    SRAL1ToL2FlushRate flushRate = new SRAL1ToL2FlushRate(dsoGlobalServerStats);
    Assert.assertEquals(StatisticType.SNAPSHOT, flushRate.getType());

    StatisticData[] statisticDatas;

    statisticDatas = flushRate.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1ToL2FlushRate.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count1 = (Long) statisticDatas[0].getData();
    Assert.eval(count1 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = flushRate.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1ToL2FlushRate.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count2 = (Long) statisticDatas[0].getData();
    Assert.eval(count2 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = flushRate.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1ToL2FlushRate.ACTION_NAME, statisticDatas[0].getName());
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