/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestServerTransactionManager implements ServerTransactionManager {

  public final NoExceptionLinkedQueue skipCalls = new NoExceptionLinkedQueue();

  public TestServerTransactionManager() {
    //
  }

  public final NoExceptionLinkedQueue shutdownClientCalls = new NoExceptionLinkedQueue();
  public final ArrayList              incomingTxnContexts = new ArrayList();
  public final List                   incomingTxns        = new ArrayList();

  public void shutdownNode(NodeID deadClient) {
    shutdownClientCalls.put(deadClient);
  }

  public void addWaitingForAcknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee) {
    throw new ImplementMe();

  }

  public boolean isWaiting(NodeID waiter, TransactionID requestID) {
    throw new ImplementMe();
  }

  public void acknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee) {
    throw new ImplementMe();
  }

  public void broadcasted(NodeID waiter, TransactionID requestID) {
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

  public void incomingTransactions(NodeID nodeID, Set txnIDs, Collection txns, boolean relayed) {
    incomingTxnContexts.add(new Object[] { nodeID, txnIDs, Boolean.valueOf(relayed) });
    incomingTxns.addAll(txns);
  }

  public void transactionsRelayed(NodeID node, Set serverTxnIDs) {
    throw new ImplementMe();
  }

  public void commit(PersistenceTransactionProvider ptxp, Collection objects, Map newRoots,
                     Collection appliedServerTransactionIDs ) {
    // NOP
  }

  public void setResentTransactionIDs(NodeID source, Collection transactionIDs) {
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

  public void nodeConnected(NodeID nodeID) {
    throw new ImplementMe();
  }

  public String dump() {
    throw new ImplementMe();
  }

  public void dump(Writer writer) {
    throw new ImplementMe();
    
  }

  public void dumpToLogger() {
    throw new ImplementMe();
    
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }

  public int getTotalPendingTransactionsCount() {
    throw new ImplementMe();
  }
}
