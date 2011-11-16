/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Maintains the transaction accounting on a passive server. It primaries keeps track of incoming transactions and those
 * transactions being applied on the passive server.
 * 
 * @author Saravanan Subbiah
 * @author Nabib El-Rahman
 */
public class PassiveTransactionAccount implements TransactionAccount {

  private final NodeID                                     nodeID;
  private final Map<ServerTransactionID, TransactionState> txnIDsToState = new HashMap<ServerTransactionID, TransactionState>();
  private CallBackOnComplete                               callback;
  private boolean                                          dead          = false;

  public PassiveTransactionAccount(final NodeID source) {
    this.nodeID = source;
  }

  /**
   * {@inheritDoc}
   */
  public NodeID getNodeID() {
    return nodeID;
  }

  /**
   * {@inheritDoc}
   */
  public boolean applyCommitted(final TransactionID requestID) {
    synchronized (this.txnIDsToState) {
      ServerTransactionID tempServerTransactionID = new ServerTransactionID(this.nodeID, requestID);
      TransactionState state = txnIDsToState.get(tempServerTransactionID);
      state.applyCommitted();
      if (state.isComplete()) {
        this.txnIDsToState.remove(tempServerTransactionID);
        invokeCallBackOnCompleteIfNecessary();
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean skipApplyAndCommit(final TransactionID requestID) {
    synchronized (this.txnIDsToState) {
      ServerTransactionID tempServerTransactionID = new ServerTransactionID(this.nodeID, requestID);
      TransactionState state = txnIDsToState.get(tempServerTransactionID);
      state.applyAndCommitSkipped();
      if (state.isComplete()) {
        this.txnIDsToState.remove(tempServerTransactionID);
        invokeCallBackOnCompleteIfNecessary();
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void addAllPendingServerTransactionIDsTo(Set<ServerTransactionID> txnsInSystem) {
    synchronized (txnIDsToState) {
      txnsInSystem.addAll(txnIDsToState.keySet());
    }
  }

  public void addObjectsSyncedTo(final NodeID to, final TransactionID txnID) {
    throw new AssertionError("Objects can't be synced from Passive Server");
  }

  /**
   * {@inheritDoc}
   */
  public void incomingTransactions(Set<ServerTransactionID> serverTxnsIDs) {
    synchronized (txnIDsToState) {
      for (ServerTransactionID transactionID : serverTxnsIDs) {
        if (!txnIDsToState.containsKey(transactionID)) {
          txnIDsToState.put(transactionID, new TransactionState(TransactionState.PASSIVE_START_STATE));
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void nodeDead(CallBackOnComplete callBack) {
    synchronized (txnIDsToState) {
      this.callback = callBack;
      this.dead = true;
      invokeCallBackOnCompleteIfNecessary();
    }
  }

  /**
   * Always returns first since {@link PassiveTransactionAccount} doesn't support waitee accounting.
   * 
   * @param TransactionID requestID
   * @return boolean
   */
  public boolean hasWaitees(TransactionID requestID) {
    return false;
  }

  /**
   * @param NodeID waitee
   * @param TransactionID requestID
   * @throws AssertionError always, should not be called.
   */
  public void addWaitee(NodeID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server : " + waitee + " , "
                             + requestID);
  }

  /**
   * @param TransactionID requestID
   * @return boolean
   * @throws AssertionError always, should not be called.
   */
  public boolean broadcastCompleted(TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server");
  }

  /**
   * @param TransactionID requestID
   * @return boolean true when the TransactionState for the requestID is complete.
   */
  public boolean processMetaDataCompleted(TransactionID requestID) {
    synchronized (this.txnIDsToState) {
      ServerTransactionID tempServerTransactionID = new ServerTransactionID(this.nodeID, requestID);
      TransactionState state = txnIDsToState.get(tempServerTransactionID);
      state.processMetaDataCompleted();
      if (state.isComplete()) {
        this.txnIDsToState.remove(tempServerTransactionID);
        invokeCallBackOnCompleteIfNecessary();
        return true;
      }
    }
    return false;
  }

  /**
   * @param TransactionID requestID
   * @return boolean
   * @throws AssertionError always, should not be called.
   */
  public boolean removeWaitee(NodeID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be ACKED to Passive Server");
  }

  /**
   * @param TransactionID requestID
   * @return boolean
   * @throws AssertionError always, should not be called.
   */
  public boolean relayTransactionComplete(TransactionID requestID) {
    throw new AssertionError("Transactions should never be relayed from Passive Server");
  }

  /**
   * Returns empty collection since {@link PassiveTransactionAccount} does not do waitee accounting.
   * 
   * @param NodeID waitee
   * @return Set set
   */
  public Set requestersWaitingFor(NodeID waitee) {
    return Collections.EMPTY_SET;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    synchronized (this.txnIDsToState) {
      stringBuilder.append("PassiveTransactionAccount [ " + this.nodeID + " ] = \n");
      for (final Entry<ServerTransactionID, TransactionState> entry : txnIDsToState.entrySet()) {
        stringBuilder.append("{").append(entry.getKey()).append(": ");
        stringBuilder.append(entry.getValue()).append("}\n");
      }
      return stringBuilder.toString();
    }
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (dead && txnIDsToState.isEmpty()) {
      callback.onComplete(nodeID);
    }
  }

}
