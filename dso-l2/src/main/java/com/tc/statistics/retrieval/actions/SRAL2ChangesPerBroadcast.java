/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.Assert;

import java.math.BigDecimal;

/**
 * This statistics gives the average number of changes that are present per broadcast.
 * <p/>
 * Number of changes and number of broadcast are sampled every second presently and this gives the average number of
 * changes per broadcast.
 */
public class SRAL2ChangesPerBroadcast implements StatisticRetrievalAction {

  public static final String       ACTION_NAME = "l2 changes per broadcast";

  private final SampledRateCounter changesPerBroadcastCounter;

  public SRAL2ChangesPerBroadcast(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    this.changesPerBroadcastCounter = serverStats.getChangesPerBroadcastCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    BigDecimal value = new BigDecimal(this.changesPerBroadcastCounter.getMostRecentSample().getCounterValue());
    return new StatisticData[] { new StatisticData(ACTION_NAME, value) };
  }
}
