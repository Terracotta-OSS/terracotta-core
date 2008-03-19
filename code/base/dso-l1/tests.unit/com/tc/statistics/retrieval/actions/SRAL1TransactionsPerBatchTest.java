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

import java.math.BigDecimal;

public class SRAL1TransactionsPerBatchTest extends TCTestCase {
  private CounterIncrementer txnsCounterIncrementer;
  private CounterIncrementer batchesCounterIncrementer;
  private SampledCounter txnCounter;
  private SampledCounter batchesCounter;


  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 10, true, 0L);
    txnCounter = (SampledCounter)counterManager.createCounter(sampledCounterConfig);
    batchesCounter = (SampledCounter)counterManager.createCounter(sampledCounterConfig);

    batchesCounterIncrementer = new CounterIncrementer(batchesCounter, 200);
    txnsCounterIncrementer = new CounterIncrementer(txnCounter, 200);
    new Thread(txnsCounterIncrementer, "Transaction Counter Incrementer").start();
    new Thread(batchesCounterIncrementer, "Batches Counter Incrementer").start();
  }


  public void testRetrieval() {
    SRAL1TransactionsPerBatch transactionsPerBatch = new SRAL1TransactionsPerBatch(txnCounter, batchesCounter);
    Assert.assertEquals(StatisticType.SNAPSHOT, transactionsPerBatch.getType());
    StatisticData[] statisticDatas;

    statisticDatas = transactionsPerBatch.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionsPerBatch.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count1 = (BigDecimal)statisticDatas[0].getData();
    Assert.eval(count1.doubleValue() >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = transactionsPerBatch.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionsPerBatch.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count2 = (BigDecimal)statisticDatas[0].getData();
    Assert.eval(count2.doubleValue() >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = transactionsPerBatch.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1TransactionsPerBatch.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count3 = (BigDecimal)statisticDatas[0].getData();
    Assert.eval(count3.doubleValue() >= 0);
  }

  protected void tearDown() throws Exception {
    txnsCounterIncrementer.stopCounterIncrement();
    txnsCounterIncrementer = null;
    batchesCounterIncrementer.stopCounterIncrement();
    batchesCounterIncrementer = null;
  }
}