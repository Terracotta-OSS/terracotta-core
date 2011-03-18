/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.Counter;
import com.tc.util.Assert;

/**
 * This statistic gives the number of outstanding batches in the L1 node.
 * <p/>
 * This statistic gives the number of outstanding batches that has been sent for processing to the server (L2) but for
 * which no acknowledgement has been received yet. This value is sampled every second by the
 * {@link com.tc.statistics.retrieval.StatisticsRetriever} at the global frequency.
 */
public class SRAL1OutstandingBatches implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l1 outstanding batches";

  private final Counter      outstandingBatchesCounter;

  public SRAL1OutstandingBatches(Counter outstandingBatchesCounter) {
    this.outstandingBatchesCounter = outstandingBatchesCounter;
    Assert.assertNotNull(outstandingBatchesCounter);
  }

  public StatisticData[] retrieveStatisticData() {
    long value = outstandingBatchesCounter.getValue();
    return new StatisticData[] { new StatisticData(ACTION_NAME, Long.valueOf(value)) };
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}
