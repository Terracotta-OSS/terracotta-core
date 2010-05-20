/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestRemoteTransactionManager implements RemoteTransactionManager {
  public final NoExceptionLinkedQueue isChangeListenerCalls = new NoExceptionLinkedQueue();

  public List                         ids;
  public Map                          changes;
  public TransactionID                txID;
  public Map                          newRoots;
  public TransactionID                acked;
  public TxnBatchID                   batchAcked;
  public ClientTransaction            transaction;

  public void commit(final ClientTransaction txn) {
    this.ids = txn.getAllLockIDs();
    this.changes = txn.getChangeBuffers();
    this.txID = txn.getTransactionID();
    this.newRoots = txn.getNewRoots();
    this.transaction = txn;
  }

  public TransactionBuffer receivedAcknowledgement(final SessionID sessionID, final TransactionID ackTxID,
                                                   final NodeID nodeID) {
    this.acked = ackTxID;
    return null;
  }

  public void receivedBatchAcknowledgement(final TxnBatchID batchID, final NodeID nodeID) {
    this.batchAcked = batchID;
  }

  public void flush(final LockID lockID) {
    throw new ImplementMe();
  }

  public void stop() {
    throw new ImplementMe();

  }

  public boolean asyncFlush(final LockID lockID, final LockFlushCallback callback) {
    throw new ImplementMe();
  }

  public void stopProcessing() {
    throw new ImplementMe();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();
  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }

  public void shutdown() {
    // NOP
  }

  public void waitForAllCurrentTransactionsToComplete() {
    //
  }

  public void waitForServerToReceiveTxnsForThisLock(LockID lock) {
    throw new ImplementMe();
  }

  public void batchReceived(TxnBatchID batchId, Set<TransactionID> set, NodeID nid) {
    throw new ImplementMe();

  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }
}
