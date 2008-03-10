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

import java.math.BigDecimal;

public class SRAL2ChangesPerBroadcast implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l2 changes per broadcast";

  private final SampledCounter changeCounter;
  private final SampledCounter broadcastCounter;

  public SRAL2ChangesPerBroadcast(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    this.changeCounter = serverStats.getChangesCounter();
    this.broadcastCounter = serverStats.getBroadcastCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue changes = changeCounter.getMostRecentSample();
    TimeStampedCounterValue broadcasts = broadcastCounter.getMostRecentSample();
    long numBroadcast = broadcasts.getCounterValue();
    long numChanges = changes.getCounterValue();
    BigDecimal value = numBroadcast != 0 ? new BigDecimal((double)numChanges / numBroadcast) : new BigDecimal(0);
    return new StatisticData[] { new StatisticData(ACTION_NAME, value) }; 
  }
}

