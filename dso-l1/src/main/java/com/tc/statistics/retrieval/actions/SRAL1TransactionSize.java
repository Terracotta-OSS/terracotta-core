/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.Assert;

/**
 * This statistic gives the average size of a transaction that is happening in an L1.
 * <p/>
 * This statistic is calculated by sampling the batchsize every second and also finding out the number of transactions
 * that is there in the batch. The batch size is divided with the number of transactions in the batch to give this
 * statistic.
 */
public class SRAL1TransactionSize implements StatisticRetrievalAction {

  public static final String       ACTION_NAME = "l1 transaction size";

  private final SampledRateCounter transactionSizeCounter;

  public SRAL1TransactionSize(final SampledRateCounter transactionSizeCounter) {
    Assert.assertNotNull(transactionSizeCounter);
    this.transactionSizeCounter = transactionSizeCounter;
  }

  public StatisticData[] retrieveStatisticData() {
    return new StatisticData[] { new StatisticData(ACTION_NAME, transactionSizeCounter.getMostRecentSample()
        .getCounterValue()) };
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}
