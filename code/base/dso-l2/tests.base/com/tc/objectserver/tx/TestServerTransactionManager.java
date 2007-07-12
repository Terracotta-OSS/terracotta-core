/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class TestServerTransactionManager implements ServerTransactionManager {

  public final NoExceptionLinkedQueue skipCalls = new NoExceptionLinkedQueue();

  public TestServerTransactionManager() {
    //
  }

  public final NoExceptionLinkedQueue shutdownClientCalls = new NoExceptionLinkedQueue();
  public final ArrayList              incomingTxnContexts = new ArrayList();

  public void shutdownClient(ChannelID deadClient) {
    shutdownClientCalls.put(deadClient);
  }

  public void addWaitingForAcknowledgement(ChannelID waiter, TransactionID requestID, ChannelID waitee) {
    throw new ImplementMe();

  }

  public boolean isWaiting(ChannelID waiter, TransactionID requestID) {
    throw new ImplementMe();
  }

  public void acknowledgement(ChannelID waiter, TransactionID requestID, ChannelID waitee) {
    throw new ImplementMe();
  }

  public void dump() {
    throw new ImplementMe();
  }

  public void broadcasted(ChannelID waiter, TransactionID requestID) {
    // NOP
  }

  public void skipApplyAndCommit(ServerTransaction txn) {
    skipCalls.put(txn);
  }

  public void addTransactionListener(ServerTransactionListener listener) {
    // NOP
  }

  public void removeTransactionListener(ServerTransactionListener listener) {
    // NOP
  }

  public void apply(ServerTransaction txn, Map objects, BackReferences includeIDs, ObjectInstanceMonitor instanceMonitor) {
    // NOP
  }

  public void incomingTransactions(ChannelID channelID, Set txnIDs, Collection txns, boolean relayed,
                                   Collection completedTxns) {
    incomingTxnContexts.add(new Object[] { channelID, txnIDs, Boolean.valueOf(relayed) });
  }

  public void transactionsRelayed(ChannelID channelID, Set serverTxnIDs) {
    throw new ImplementMe();
  }

  public void commit(PersistenceTransactionProvider ptxp, Collection objects, Map newRoots,
                     Collection appliedServerTransactionIDs, Set completedTransactionIDs) {
    // NOP
  }

  public void setResentTransactionIDs(ChannelID channelID, Collection transactionIDs) {
    // NOP
  }

  public void start(Set cids) {
    // NOP
  }

  public void goToActiveMode() {
    throw new ImplementMe();
  }

  public void callBackOnTxnsInSystemCompletion(TxnsInSystemCompletionLister l) {
    throw new ImplementMe();
  }
}
