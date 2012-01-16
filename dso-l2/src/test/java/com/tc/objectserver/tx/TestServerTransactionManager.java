/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class TestServerTransactionManager implements ServerTransactionManager {

  public final NoExceptionLinkedQueue skipCalls = new NoExceptionLinkedQueue();

  public TestServerTransactionManager() {
    //
  }

  public final NoExceptionLinkedQueue shutdownClientCalls = new NoExceptionLinkedQueue();
  public final ArrayList              incomingTxnContexts = new ArrayList();
  public final List                   incomingTxns        = new ArrayList();

  public void shutdownNode(NodeID deadClient) {
    this.shutdownClientCalls.put(deadClient);
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
    this.skipCalls.put(txn);
  }

  public void addTransactionListener(ServerTransactionListener listener) {
    // NOP
  }

  public void removeTransactionListener(ServerTransactionListener listener) {
    // NOP
  }

  public void apply(ServerTransaction txn, Map objects, ApplyTransactionInfo includeIDs,
                    ObjectInstanceMonitor instanceMonitor) {
    // NOP
  }

  public void incomingTransactions(NodeID nodeID, Set txnIDs, Collection txns, boolean relayed) {
    this.incomingTxnContexts.add(new Object[] { nodeID, txnIDs, Boolean.valueOf(relayed) });
    this.incomingTxns.addAll(txns);
  }

  public void transactionsRelayed(NodeID node, Set serverTxnIDs) {
    throw new ImplementMe();
  }

  public void commit(PersistenceTransactionProvider ptxp, Collection<ManagedObject> objects,
                     Map<String, ObjectID> newRoots, Collection<ServerTransactionID> appliedServerTransactionIDs,
                     SortedSet<ObjectID> deletedObjects) {
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

  public void callBackOnTxnsInSystemCompletion(TxnsInSystemCompletionListener l) {
    throw new ImplementMe();
  }

  public void nodeConnected(NodeID nodeID) {
    throw new ImplementMe();
  }

  public int getTotalPendingTransactionsCount() {
    throw new ImplementMe();
  }

  public void objectsSynched(NodeID node, ServerTransactionID tid) {
    throw new ImplementMe();
  }

  public void callBackOnResentTxnsInSystemCompletion(TxnsInSystemCompletionListener l) {
    throw new ImplementMe();
  }

  public long getTotalNumOfActiveTransactions() {
    throw new ImplementMe();
  }

  public void processingMetaDataCompleted(NodeID sourceID, TransactionID txnID) {
    throw new ImplementMe();
  }

  public void processMetaData(Collection<ServerTransaction> txns) {
    //
  }
}