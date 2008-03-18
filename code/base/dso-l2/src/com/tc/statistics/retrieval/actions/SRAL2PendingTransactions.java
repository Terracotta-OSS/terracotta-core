/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.util.Assert;

/**
 * This statistic gives the global number of pending transactions in the L2.
 * <p/>
 * The overall number of pending transactions that is present in the system.
 * This value is sampled by the {@link com.tc.statistics.retrieval.StatisticsRetriever}
 * at the global frequency.
 */
public class SRAL2PendingTransactions implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l2 pending transactions";

  private final ServerTransactionManager serverTransactionManager;

  public SRAL2PendingTransactions(final ServerTransactionManager serverTransactionManager) {
    this.serverTransactionManager = serverTransactionManager;
    Assert.assertNotNull(serverTransactionManager);
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    int numPendingTxns = serverTransactionManager.getTotalPendingTransactionsCount();
    return new StatisticData[] { new StatisticData(ACTION_NAME, (long)numPendingTxns) };
  }
}