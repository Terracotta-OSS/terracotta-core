/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.tx.TransactionID;

public interface TransactionBatchManager {

  public void defineBatch(NodeID node, int numTxns) throws BatchDefinedException;

  public boolean batchComponentComplete(NodeID committerID, TransactionID txnID)
      throws NoSuchBatchException;

  public void shutdownNode(NodeID nodeID);
}