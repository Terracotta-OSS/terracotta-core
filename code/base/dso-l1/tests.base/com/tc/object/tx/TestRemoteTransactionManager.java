/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.List;
import java.util.Map;

public class TestRemoteTransactionManager implements RemoteTransactionManager {
  public final NoExceptionLinkedQueue isChangeListenerCalls = new NoExceptionLinkedQueue();

  public List                         ids;
  public Map                          changes;
  public TransactionID                txID;
  public Map                          newRoots;
  public TransactionID                acked;
  public TxnBatchID                   batchAcked;
  public ClientTransaction            transaction;

  public void commit(ClientTransaction txn) {
    this.ids = txn.getAllLockIDs();
    this.changes = txn.getChangeBuffers();
    this.txID = txn.getTransactionID();
    this.newRoots = txn.getNewRoots();
    this.transaction = txn;
  }

  public void receivedAcknowledgement(SessionID sessionID, TransactionID ackTxID, NodeID nodeID) {
    this.acked = ackTxID;
  }

  public void receivedBatchAcknowledgement(TxnBatchID batchID, NodeID nodeID) {
    this.batchAcked = batchID;
  }

  public void flush(LockID lockID) {
    throw new ImplementMe();
  }

  public void stop() {
    throw new ImplementMe();

  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    throw new ImplementMe();
  }

  public void stopProcessing() {
    throw new ImplementMe();
  }

}