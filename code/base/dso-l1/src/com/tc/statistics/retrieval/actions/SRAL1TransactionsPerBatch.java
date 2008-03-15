/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;

import java.math.BigDecimal;

/**
 * This statistic gives the average number of transactions per batch in an L1 node
 * <p/>
 * This statistic gives the average number of transactions per batch in a node. Every change in any shared object
 * in a node needs to be propagated to the server (L2) and broadcasted to all other nodes who are using this shared object.
 * Changes to shared-objects are communicated to the server in a transaction which are batched together. This statistic gives
 * the number of transactions per such batch happening in each L1 node.
 * <p/>
 * This is sampled by the {@link com.tc.statistics.retrieval.StatisticsRetriever} at the global frequency.
 */
public class SRAL1TransactionsPerBatch implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l1 transactions per batch";

  private final SampledCounter numTransactions;
  private final SampledCounter numBatches;

  public SRAL1TransactionsPerBatch(SampledCounter numTransactions, SampledCounter numBatches) {
    this.numTransactions = numTransactions;
    this.numBatches = numBatches;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue numTxnValue = numTransactions.getMostRecentSample();
    long txnValue = numTxnValue.getCounterValue();

    TimeStampedCounterValue numBatchesValue = numBatches.getMostRecentSample();
    long batchesValue = numBatchesValue.getCounterValue();

    return new StatisticData[] { new StatisticData(ACTION_NAME, batchesValue == 0 ?
      new BigDecimal(0) : new BigDecimal((double)txnValue / batchesValue)) };
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}
