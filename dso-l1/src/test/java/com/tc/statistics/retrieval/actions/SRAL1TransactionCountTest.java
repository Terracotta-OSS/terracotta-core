/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

public class SRAL1TransactionCountTest extends TCTestCase {
  private CounterIncrementer counterIncrementer;
  private SampledCounter txCounter;

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 10, true, 0L);
    txCounter = (SampledCounter)counterManager.createCounter(sampledCounterConfig);
    counterIncrementer = new CounterIncrementer(txCounter, 200);
    new Thread(counterIncrementer, "Counter Incrementer").start();
  }

  public void testRetrieval() {
    SRAL1TransactionCount txCount = new SRAL1TransactionCount(txCounter);
    Assert.assertEquals(StatisticType.SNAPSHOT, txCount.getType());
    StatisticData[] statisticDatas;

    statisticDatas = txCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionCount.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count1 = ((Long)statisticDatas[0].getData()).longValue();
    Assert.eval(count1 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = txCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionCount.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count2 = ((Long)statisticDatas[0].getData()).longValue();
    Assert.eval(count2 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = txCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionCount.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count3 = ((Long)statisticDatas[0].getData()).longValue();
    Assert.eval(count3 >= 0);
  }

  protected void tearDown() throws Exception {
    counterIncrementer.stopCounterIncrement();
    counterIncrementer = null;
  }
}
