/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Map;

/**
 * @author steve
 */
public class TestRemoteTransactionManager implements RemoteTransactionManager {
//  public final NoExceptionLinkedQueue resendOutstandingContexts = new NoExceptionLinkedQueue();
//  public final NoExceptionLinkedQueue pauseCalls                = new NoExceptionLinkedQueue();
//  public final NoExceptionLinkedQueue unpauseCalls              = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue isChangeListenerCalls     = new NoExceptionLinkedQueue();

  public LockID[]                     ids;
  public Map                          changes;
  public TransactionID                txID;
  public Map                          newRoots;
  public TransactionID                acked;
  public TxnType                      transactionType;
  public TxnBatchID                   batchAcked;
  public ClientTransaction            transaction;

  public void commit(ClientTransaction txn) {
    this.ids = txn.getAllLockIDs();
    this.changes = txn.getChangeBuffers();
    this.txID = txn.getTransactionID();
    this.newRoots = txn.getNewRoots();
    this.transactionType = txn.getTransactionType();
    this.transaction = txn;
  }

  public void receivedAcknowledgement(SessionID sessionID, TransactionID ackTxID) {
    this.acked = ackTxID;
  }

  public void receivedBatchAcknowledgement(TxnBatchID batchID) {
    this.batchAcked = batchID;
  }

  public int getPendingBatchSize() {
    throw new ImplementMe();
  }

  public void resendOutstanding() {
    //this.resendOutstandingContexts.put(new Object());
    throw new ImplementMe();
  }

  public void pause() {
    //this.pauseCalls.put(new Object());
    throw new ImplementMe();
  }

  public void unpause() {
    //this.pauseCalls.put(new Object());
    throw new ImplementMe();
  }

  public void flush(LockID lockID) {
    throw new ImplementMe();
  }

  public Collection getTransactionSequenceIDs() {
    throw new ImplementMe();
  }

  public void stop() {
    throw new ImplementMe();

  }

  public void starting() {
    throw new ImplementMe();

  }

  public Collection getResentTransactionIDs() {
    throw new ImplementMe();
  }

  public void resendOutstandingAndUnpause() {
    throw new ImplementMe();
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    throw new ImplementMe();
  }

  public void stopProcessing() {
    throw new ImplementMe();
  }
}