/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.tx.TxnBatchID;

import java.io.IOException;

public interface TransactionBatchReader {
  public ServerTransaction getNextTransaction() throws IOException;

  public TxnBatchID getBatchID();

  public int getNumTxns();

  public NodeID getNodeID();

}
