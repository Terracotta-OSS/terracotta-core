/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.Assert;

/**
 * This statistic gives the size of pending transactions in an L1 node.
 * <p/>
 * This statistic gives us the size of pending transactions present in an L1 node.
 * This value is sampled every second.
 */
public class SRAL1PendingTransactionsSize implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l1 pending transactions size";

  private final SampledCounter pendingTransactionsSize;

  public SRAL1PendingTransactionsSize(final SampledCounter pendingTransactionsSize) {
    this.pendingTransactionsSize = pendingTransactionsSize;
    Assert.assertNotNull(pendingTransactionsSize);
  }

  public StatisticData[] retrieveStatisticData() {
    final long pendingTransactions = pendingTransactionsSize.getMostRecentSample().getCounterValue();
    return new StatisticData[] { new StatisticData(ACTION_NAME, new Long(pendingTransactions)) };
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}