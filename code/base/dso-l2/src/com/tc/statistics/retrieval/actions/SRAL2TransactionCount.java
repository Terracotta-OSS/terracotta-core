/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.StatisticData;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.util.Assert;

import java.util.Date;

public class SRAL2TransactionCount implements StatisticRetrievalAction {

  public final static String ACTION_NAME = "l2 transaction count";

  private final SampledCounter txnCounter;

  public SRAL2TransactionCount(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    txnCounter = serverStats.getTransactionCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue value = txnCounter.getMostRecentSample();
    // todo: this might have to be changed into new Date(value.getTimestamp()),
    // which is when the actual sampling occurred, we use the 'now' timestamp at
    // the moment to make sure that the statistic data retrieval arrives in order.
    // Otherwise, this data entry could be timed before the startup data event of
    // the capture session.
    Date moment = new Date();
    return new StatisticData[] {new StatisticData(ACTION_NAME, moment, value.getCounterValue())};
  }
}