/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.SimpleCounterConfig;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

public class SRAL1PendingBatchesSizeTest extends TCTestCase {
  private CounterIncrementer counterIncrementer;
  private Counter            pendingBatchesCounter;

  @Override
  protected void setUp() throws Exception {
    pendingBatchesCounter = new CounterManagerImpl().createCounter(new SimpleCounterConfig(0));
    counterIncrementer = new CounterIncrementer(pendingBatchesCounter, 200);
    new Thread(counterIncrementer, "Counter Incrementer").start();
  }

  public void testRetrieval() {
    SRAL1PendingBatchesSize pendingBatches = new SRAL1PendingBatchesSize(pendingBatchesCounter);
    Assert.assertEquals(StatisticType.SNAPSHOT, pendingBatches.getType());
    StatisticData[] statisticDatas;

    statisticDatas = pendingBatches.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1PendingBatchesSize.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count1 = ((Long) statisticDatas[0].getData()).longValue();
    Assert.eval(count1 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = pendingBatches.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1PendingBatchesSize.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count2 = ((Long) statisticDatas[0].getData()).longValue();
    Assert.eval(count2 >= 0);

    ThreadUtil.reallySleep(1000);

    statisticDatas = pendingBatches.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL1PendingBatchesSize.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count3 = ((Long) statisticDatas[0].getData()).longValue();
    Assert.eval(count3 >= 0);
  }

  @Override
  protected void tearDown() throws Exception {
    counterIncrementer.stopCounterIncrement();
    counterIncrementer = null;
  }
}
