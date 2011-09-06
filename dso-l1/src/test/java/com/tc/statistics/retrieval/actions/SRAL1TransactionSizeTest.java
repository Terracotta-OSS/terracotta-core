/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

public class SRAL1TransactionSizeTest extends TCTestCase {
  private CounterIncrementer counterIncrementer;
  private SampledRateCounter transactionSizeCounter;

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledRateCounterConfig sampledCounterConfig = new SampledRateCounterConfig(1, 10, true);
    transactionSizeCounter = (SampledRateCounter) counterManager.createCounter(sampledCounterConfig);
    counterIncrementer = new CounterIncrementer(transactionSizeCounter, 200);
    new Thread(counterIncrementer, "Counter Incrementer").start();
  }

  public void testRetrieval() {
    SRAL1TransactionSize transactionsSize = new SRAL1TransactionSize(transactionSizeCounter);
    Assert.assertEquals(StatisticType.SNAPSHOT, transactionsSize.getType());
    StatisticData[] statisticDatas;

    statisticDatas = transactionsSize.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionSize.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count1 = ((Long) statisticDatas[0].getData()).longValue();
    Assert.eval(count1 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = transactionsSize.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionSize.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count2 = ((Long) statisticDatas[0].getData()).longValue();
    Assert.eval(count2 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = transactionsSize.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionSize.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count3 = ((Long) statisticDatas[0].getData()).longValue();
    Assert.eval(count3 >= 0);
  }

  protected void tearDown() throws Exception {
    counterIncrementer.stopCounterIncrement();
    counterIncrementer = null;
  }
}