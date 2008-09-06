/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.Counter;
import com.tc.util.Assert;

/**
 * This statistic gives the size of pending batches in an L1 node.
 * <p/>
 * This statistic gives us the size of pending batches present in an L1 node.
 * This value is sampled every second.
 */
public class SRAL1PendingBatchesSize implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l1 pending batches size";

  private final Counter pendingBatchesSize;

  public SRAL1PendingBatchesSize(final Counter pendingBatchesSize) {
    this.pendingBatchesSize = pendingBatchesSize;
    Assert.assertNotNull(pendingBatchesSize);
  }

  public StatisticData[] retrieveStatisticData() {
    final long pendingBatches = pendingBatchesSize.getValue();
    return new StatisticData[] { new StatisticData(ACTION_NAME, new Long(pendingBatches)) };
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}