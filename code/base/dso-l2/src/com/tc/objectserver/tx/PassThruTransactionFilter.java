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

  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    transactionBatchManager = scc.getTransactionBatchManager();
  }

  public void addTransactionBatch(TransactionBatchContext transactionBatchContext) {
    transactionBatchManager.processTransactions(transactionBatchContext);
  }

  public boolean shutdownNode(NodeID nodeID) {
    return true;
  }
}