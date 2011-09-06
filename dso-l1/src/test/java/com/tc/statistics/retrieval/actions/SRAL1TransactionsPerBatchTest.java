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

import java.math.BigDecimal;

public class SRAL1TransactionsPerBatchTest extends TCTestCase {
  private SampledRateCounter transactionsPerBatchCounter;
  private CounterIncrementer counterIncrementer;

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledRateCounterConfig sampledCounterConfig = new SampledRateCounterConfig(1, 10, true);
    transactionsPerBatchCounter = (SampledRateCounter) counterManager.createCounter(sampledCounterConfig);

    counterIncrementer = new CounterIncrementer(transactionsPerBatchCounter, 200);
    new Thread(counterIncrementer, "Transaction Counter Incrementer").start();
  }

  public void testRetrieval() {
    SRAL1TransactionsPerBatch transactionsPerBatch = new SRAL1TransactionsPerBatch(transactionsPerBatchCounter);
    Assert.assertEquals(StatisticType.SNAPSHOT, transactionsPerBatch.getType());
    StatisticData[] statisticDatas;

    statisticDatas = transactionsPerBatch.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionsPerBatch.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count1 = (BigDecimal) statisticDatas[0].getData();
    Assert.eval(count1.doubleValue() >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = transactionsPerBatch.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionsPerBatch.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count2 = (BigDecimal) statisticDatas[0].getData();
    Assert.eval(count2.doubleValue() >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = transactionsPerBatch.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionsPerBatch.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count3 = (BigDecimal) statisticDatas[0].getData();
    Assert.eval(count3.doubleValue() >= 0);
  }

  protected void tearDown() throws Exception {
    counterIncrementer.stopCounterIncrement();
    counterIncrementer = null;
  }
}