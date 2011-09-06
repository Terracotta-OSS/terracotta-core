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
import com.tc.util.Assert;

/**
 * This statistics gives the cumulative count of server map GET_SIZE requests.
 */
public class SRAL2ServerMapGetSizeRequestsCount implements StatisticRetrievalAction {

  public final static String             ACTION_NAME = "l2 servermap GET_SIZE requests count";

  private final SampledCumulativeCounter getSizeRequestsCount;

  public SRAL2ServerMapGetSizeRequestsCount(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    getSizeRequestsCount = serverStats.getServerMapGetSizeRequestsCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    return new StatisticData[] { new StatisticData(ACTION_NAME, getSizeRequestsCount.getCumulativeValue()) };
  }
}