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

import java.math.BigDecimal;

public class SRAL2BroadcastPerTransactionTest extends TestCase {

  private DSOGlobalServerStats dsoGlobalServerStats;
  private CounterIncrementer   broadcastCounterIncrementer;
  private CounterIncrementer   txnCounterIncrementer;

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 10, true, 0L);
    final SampledCounter broadcastCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);
    final SampledCounter transactionCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);

    dsoGlobalServerStats = new DSOGlobalServerStatsImpl(null, null, transactionCounter, null, broadcastCounter, null,
                                                        null, null, null, null, null, null);

    txnCounterIncrementer = new CounterIncrementer(transactionCounter, 200);
    broadcastCounterIncrementer = new CounterIncrementer(broadcastCounter, 200);
    new Thread(broadcastCounterIncrementer, "Broadcast Counter Incrementer").start();
    new Thread(txnCounterIncrementer, "Transaction Counter Incrementer").start();
  }

  public void testRetrieval() {
    SRAL2BroadcastPerTransaction sral2BroadcastPerTransaction = new SRAL2BroadcastPerTransaction(dsoGlobalServerStats);
    Assert.assertEquals(StatisticType.SNAPSHOT, sral2BroadcastPerTransaction.getType());

    StatisticData[] statisticDatas;

    statisticDatas = sral2BroadcastPerTransaction.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2BroadcastPerTransaction.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count1 = (BigDecimal) statisticDatas[0].getData();
    Assert.eval(count1.doubleValue() >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = sral2BroadcastPerTransaction.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2BroadcastPerTransaction.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count2 = (BigDecimal) statisticDatas[0].getData();
    Assert.eval(count2.doubleValue() >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = sral2BroadcastPerTransaction.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2BroadcastPerTransaction.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    BigDecimal count3 = (BigDecimal) statisticDatas[0].getData();
    Assert.eval(count3.doubleValue() >= 0);
  }

  protected void tearDown() throws Exception {
    broadcastCounterIncrementer.stopCounterIncrement();
    broadcastCounterIncrementer = null;
    txnCounterIncrementer.stopCounterIncrement();
    txnCounterIncrementer = null;
    dsoGlobalServerStats = null;
  }
}
