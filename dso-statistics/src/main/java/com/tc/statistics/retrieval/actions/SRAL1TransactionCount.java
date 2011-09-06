/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.util.Assert;

public class SRAL1TransactionCount implements StatisticRetrievalAction {

  public static final String   ACTION_NAME = "l1 transaction count";

  private final SampledCounter txnCounter;

  public SRAL1TransactionCount(final SampledCounter txnCounter) {
    Assert.assertNotNull("txnCounter", txnCounter);
    this.txnCounter = txnCounter;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue value = txnCounter.getMostRecentSample();
    return new StatisticData[] { new StatisticData(ACTION_NAME, value.getCounterValue()) };
  }
}
