/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.exception.TCRuntimeException;
import com.tc.net.groups.NodeID;
import com.tc.object.tx.TransactionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

public final class TestTransactionBatchManager implements TransactionBatchManager {

  public final LinkedQueue defineBatchContexts = new LinkedQueue();

  public void defineBatch(NodeID node, int numTxns) {
    Object[] args = new Object[] { node, new Integer(numTxns) };
    try {
      defineBatchContexts.put(args);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public final NoExceptionLinkedQueue batchComponentCompleteCalls = new NoExceptionLinkedQueue();
  public final SynchronizedBoolean    isBatchComponentComplete    = new SynchronizedBoolean(false);

  public boolean batchComponentComplete(NodeID committerID, TransactionID txnID) {
    batchComponentCompleteCalls.put(new Object[] { committerID, txnID });
    return isBatchComponentComplete.get();
  }

  public final NoExceptionLinkedQueue shutdownClientCalls = new NoExceptionLinkedQueue();

  public void shutdownNode(NodeID nodeID) {
    shutdownClientCalls.put(nodeID);
  }

}