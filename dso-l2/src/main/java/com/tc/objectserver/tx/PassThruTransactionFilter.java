/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.net.NodeID;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class PassThruTransactionFilter implements TransactionFilter, PostInit {

  private TransactionBatchManager transactionBatchManager;

  @Override
  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionBatchManager = scc.getTransactionBatchManager();
  }

  @Override
  public void addTransactionBatch(final TransactionBatchContext transactionBatchContext) {
    this.transactionBatchManager.processTransactions(transactionBatchContext);
  }

  @Override
  public boolean shutdownNode(final NodeID nodeID) {
    return true;
  }

  @Override
  public void notifyServerHighWaterMark(final NodeID nodeID, final long serverHighWaterMark) {
    // NOP
  }
}