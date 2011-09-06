/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.tx.ServerTransactionSequencerStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.util.Assert;

public class SRAServerTransactionSequencer implements StatisticRetrievalAction {

  public final static String                    ACTION_NAME           = "l2 transaction sequencer";

  public final static String                    TXN_COUNT             = "txn count";

  public final static String                    PENDING_TXN_COUNT     = "pending transactions count";

  public final static String                    BLOCKED_TXN_COUNT     = "blocked transactions count";

  public final static String                    BLOCKED_OBJECTS_COUNT = "blocked objects count";

  private final ServerTransactionSequencerStats serverTransactionSequencerStats;

  public SRAServerTransactionSequencer(final ServerTransactionSequencerStats serverTransactionSequencerStats) {
    Assert.assertNotNull("serverTransactionSequencerStats", serverTransactionSequencerStats);
    this.serverTransactionSequencerStats = serverTransactionSequencerStats;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    return new StatisticData[] {
        new StatisticData(ACTION_NAME, TXN_COUNT, (long) serverTransactionSequencerStats.getTxnsCount()),
        new StatisticData(ACTION_NAME, PENDING_TXN_COUNT, (long) serverTransactionSequencerStats.getPendingTxnsCount()),
        new StatisticData(ACTION_NAME, BLOCKED_TXN_COUNT, (long) serverTransactionSequencerStats.getBlockedTxnsCount()),
        new StatisticData(ACTION_NAME, BLOCKED_OBJECTS_COUNT, (long) serverTransactionSequencerStats
            .getBlockedObjectsCount()) };

  }
}