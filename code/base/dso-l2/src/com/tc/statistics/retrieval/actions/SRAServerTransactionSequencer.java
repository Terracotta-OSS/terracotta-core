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

  public final static String                    ACTION_NAME       = "server transaction sequencer stats";

  private final static String                   DUMP_BLOCKEDQ     = "dumpBlockedQ";

  private final static String                   DUMP_TXNQ         = "dumpTxnQ";

  private final static String                   DUMP_LOCKS        = "dumpLocks";

  private final static String                   DUMP_OBJECTS      = "dumpObjects";

  private final static String                   DUMP_PENDING_TXNS = "dumpPendingTxns";

  private final static String                   RECONCILE_STATUS  = "reconcileStatus";

  private final ServerTransactionSequencerStats serverTransactionSequencerStats;

  public SRAServerTransactionSequencer(final ServerTransactionSequencerStats serverTransactionSequencerStats) {
    Assert.assertNotNull("serverTransactionSequencerStats", serverTransactionSequencerStats);
    this.serverTransactionSequencerStats = serverTransactionSequencerStats;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.TRIGGERED;
  }

  public StatisticData[] retrieveStatisticData() {
    return new StatisticData[] { new StatisticData(DUMP_BLOCKEDQ, serverTransactionSequencerStats.dumpBlockedQ()),
        new StatisticData(DUMP_TXNQ, serverTransactionSequencerStats.dumpTxnQ()),
        new StatisticData(DUMP_LOCKS, serverTransactionSequencerStats.dumpLocks()),
        new StatisticData(DUMP_OBJECTS, serverTransactionSequencerStats.dumpObjects()),
        new StatisticData(DUMP_PENDING_TXNS, serverTransactionSequencerStats.dumpPendingTxns()),
        new StatisticData(RECONCILE_STATUS, serverTransactionSequencerStats.reconcileStatus()) };

  }
}