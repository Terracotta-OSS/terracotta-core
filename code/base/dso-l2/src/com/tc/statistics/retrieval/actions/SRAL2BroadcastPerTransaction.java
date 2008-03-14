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

/**
 * This statistics gives the number of broadcasts happening per transaction.
 * <p/>
 * Both the number of broadcasts and the number of transactions are sampled and
 * this gives the average number of broadcasts that is happening per transaction
 */
public class SRAL2BroadcastPerTransaction implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l2 broadcast per transaction";

  private final SampledCounter txnCounter;
  private final SampledCounter broadcastCounter;

  public SRAL2BroadcastPerTransaction(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    this.txnCounter = serverStats.getTransactionCounter();
    this.broadcastCounter = serverStats.getBroadcastCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue numTxn = txnCounter.getMostRecentSample();
    TimeStampedCounterValue numBroadcasts = broadcastCounter.getMostRecentSample();
    BigDecimal value = numTxn.getCounterValue() != 0 ? new BigDecimal((double)numBroadcasts.getCounterValue() /
                                                                      numTxn.getCounterValue()) : new BigDecimal(0);
    return new StatisticData[] { new StatisticData(ACTION_NAME, value) };
  }
}
