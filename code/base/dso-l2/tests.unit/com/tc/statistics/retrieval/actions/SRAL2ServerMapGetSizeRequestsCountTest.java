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

import java.util.Random;

import junit.framework.TestCase;

public class SRAL2ServerMapGetSizeRequestsCountTest extends TestCase {

  private static final Random      random = new Random(System.currentTimeMillis());

  private DSOGlobalServerStatsImpl dsoGlobalServerStats;
  private SampledCumulativeCounter getSizeCounter;

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCumulativeCounterConfig sampledCounterConfig = new SampledCumulativeCounterConfig(1, 10, true, 0L);
    getSizeCounter = (SampledCumulativeCounter) counterManager.createCounter(sampledCounterConfig);
    dsoGlobalServerStats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null, null, null,
                                                        null, null);
    dsoGlobalServerStats.serverMapGetSizeRequestsCounter(getSizeCounter);
  }

  public void testRetrieval() {
    SRAL2ServerMapGetSizeRequestsCount sral2GetSizeRequestsCount = new SRAL2ServerMapGetSizeRequestsCount(
                                                                                                          dsoGlobalServerStats);
    Assert.assertEquals(StatisticType.SNAPSHOT, sral2GetSizeRequestsCount.getType());

    StatisticData[] statisticDatas;

    statisticDatas = sral2GetSizeRequestsCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2ServerMapGetSizeRequestsCount.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count1 = (Long) statisticDatas[0].getData();
    Assert.eval(count1 == 0);

    int delta = random.nextInt(10) + 1;
    getSizeCounter.increment(delta);

    statisticDatas = sral2GetSizeRequestsCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2ServerMapGetSizeRequestsCount.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count2 = (Long) statisticDatas[0].getData();
    Assert.eval(count2 == delta);

    int delta1 = random.nextInt(10) + 1;
    getSizeCounter.increment(delta1);

    statisticDatas = sral2GetSizeRequestsCount.retrieveStatisticData();
    Assert.assertEquals(1, statisticDatas.length);
    Assert.assertEquals(SRAL2ServerMapGetSizeRequestsCount.ACTION_NAME, statisticDatas[0].getName());
    Assert.assertNull(statisticDatas[0].getAgentIp());
    Assert.assertNull(statisticDatas[0].getAgentDifferentiator());
    long count3 = (Long) statisticDatas[0].getData();
    Assert.eval(count3 == delta + delta1);
  }

  protected void tearDown() throws Exception {
    dsoGlobalServerStats = null;
  }
}
