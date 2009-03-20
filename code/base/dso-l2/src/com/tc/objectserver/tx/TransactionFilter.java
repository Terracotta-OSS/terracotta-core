/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;

public interface TransactionFilter {

  public void addTransactionBatch(TransactionBatchContext transactionBatchContext);

  /**
   * The Filter returns true if the node can be disconnected immediately and the rest of the managers can be notified of
   * disconnect immediately. If not the filter calls back at a later time when it deems good.
   */
  public boolean shutdownNode(NodeID nodeID);

  public void notifyServerHighWaterMark(NodeID nodeID, long serverHighWaterMark);

}
