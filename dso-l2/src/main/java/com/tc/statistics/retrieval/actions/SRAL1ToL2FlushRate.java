/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.util.Assert;

/**
 * This statistic gives the global flush rate of objects from L1 to L2 happening per second.
 * <p/>
 * This statistic gives the number of objects removed (flushed) from the client memory.
 * The number of object flush that is happening is sampled every second and the {@link com.tc.statistics.retrieval.StatisticsRetriever} samples this data at the global frequency.
 */
public class SRAL1ToL2FlushRate implements StatisticRetrievalAction {

  public final static String ACTION_NAME = "l1 l2 flush";

  private final SampledCounter counter;

  public SRAL1ToL2FlushRate(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    counter = serverStats.getObjectFlushCounter();
    Assert.assertNotNull("counter", counter);
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue value = counter.getMostRecentSample();
    return new StatisticData[] { new StatisticData(ACTION_NAME, value.getCounterValue()) };
  }
}