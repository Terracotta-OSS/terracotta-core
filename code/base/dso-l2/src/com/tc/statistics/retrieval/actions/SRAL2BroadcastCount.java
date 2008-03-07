/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.util.Assert;

import java.util.Date;

public class SRAL2BroadcastCount implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l2 broadcast count";

  private final SampledCounter broadcastCounter;

  public SRAL2BroadcastCount(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    broadcastCounter = serverStats.getBroadcastCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue value = broadcastCounter.getMostRecentSample();
    // todo: this might have to be changed into new Date(value.getTimestamp()),
    // which is when the actual sampling occurred, we use the 'now' timestamp at
    // the moment to make sure that the statistic data retrieval arrives in order.
    // Otherwise, this data entry could be timed before the startup data event of
    // the capture session.
    Date moment = new Date();
    return new StatisticData[] {new StatisticData(ACTION_NAME, moment, value.getCounterValue())};
  }
}
