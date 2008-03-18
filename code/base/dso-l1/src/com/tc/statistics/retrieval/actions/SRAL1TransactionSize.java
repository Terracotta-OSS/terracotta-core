/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.Assert;

import java.math.BigDecimal;

/**
 * This statistic gives the average size of a transaction that is happening in an L1.
 * <p/>
 * This statistic is calculated by sampling the batchsize every second and also finding
 * out the number of transactions that is there in the batch. The batch size is divided
 * with the number of transactions in the batch to give this statistic.
 */
public class SRAL1TransactionSize implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l1 transaction size";

  private final SampledCounter batchSizeCounter;
  private final SampledCounter numTransactionsCounter;

  public SRAL1TransactionSize(final SampledCounter batchSizeCounter, final SampledCounter numTransactionsCounter) {
    this.batchSizeCounter = batchSizeCounter;
    this.numTransactionsCounter = numTransactionsCounter;
    Assert.assertNotNull(batchSizeCounter);
    Assert.assertNotNull(numTransactionsCounter);
  }

  public StatisticData[] retrieveStatisticData() {
    final long numTrans = numTransactionsCounter.getMostRecentSample().getCounterValue();
    final long batchSize = batchSizeCounter.getMostRecentSample().getCounterValue();

    return new StatisticData[] { new StatisticData(ACTION_NAME, numTrans == 0 ? new BigDecimal(0) :
      new BigDecimal((double)batchSize / numTrans)) };
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}
