/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.util.Assert;

/**
 * This statistics gives the server map GET_VALUE requests happening per second in the L2.
 * <p/>
 * The number of server map GET_VALUE requests happening is sampled every second and the
 * {@link com.tc.statistics.retrieval.StatisticsRetriever} samples this data at the global frequency.
 */
public class SRAL2ServerMapGetValueRequestsRate implements StatisticRetrievalAction {

  public final static String             ACTION_NAME = "l2 servermap GET_VALUE requests rate";

  private final SampledCumulativeCounter getValueRequestsCounter;

  public SRAL2ServerMapGetValueRequestsRate(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    getValueRequestsCounter = serverStats.getServerMapGetValueRequestsCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue value = getValueRequestsCounter.getMostRecentSample();
    return new StatisticData[] { new StatisticData(ACTION_NAME, value.getCounterValue()) };
  }
}