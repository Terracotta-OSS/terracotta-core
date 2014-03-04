/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Util;

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
  private final Map<TransactionID, TransactionState> txnIDsToState = new HashMap<TransactionID, TransactionState>();
  private CallBackOnComplete                               callback;
  private boolean                                          dead          = false;

  public PassiveTransactionAccount(final NodeID source) {
    this.nodeID = source;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeID getNodeID() {
    return nodeID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized boolean applyCommitted(final TransactionID requestID) {
    TransactionState state = txnIDsToState.get(requestID);
    state.applyCommitted();
    notifyAll();
    if (state.isComplete()) {
      this.txnIDsToState.remove(requestID);
      invokeCallBackOnCompleteIfNecessary();
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized boolean skipApplyAndCommit(final TransactionID requestID) {
    TransactionState state = txnIDsToState.get(requestID);
    state.applyAndCommitSkipped();
    notifyAll();
    if (state.isComplete()) {
      this.txnIDsToState.remove(requestID);
      invokeCallBackOnCompleteIfNecessary();
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void addAllPendingServerTransactionIDsTo(Set<ServerTransactionID> txnsInSystem) {
    txnsInSystem.addAll(Collections2.transform(txnIDsToState.keySet(), new Function<TransactionID, ServerTransactionID>() {
      @Override
      public ServerTransactionID apply(final TransactionID input) {
        return new ServerTransactionID(nodeID, input);
      }
    }));
  }

  @Override
  public void addObjectsSyncedTo(final NodeID to, final TransactionID txnID) {
    throw new AssertionError("Objects can't be synced from Passive Server");
  }

  @Override
  public void waitUntilRelayed(final TransactionID txnID) {
    // Do nothing, we don't need to relay for a passive.
  }

  @Override
  public synchronized void waitUntilCommitted(final TransactionID txnID) {
    boolean interrupted = false;
    while (true) {
      TransactionState state = txnIDsToState.get(txnID);
      if (state == null || state.isApplyCommitted()) {
        break;
      }
      try {
        wait();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(interrupted);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void incomingTransactions(Set<ServerTransactionID> serverTxnsIDs) {
    for (ServerTransactionID transactionID : serverTxnsIDs) {
      if (!txnIDsToState.containsKey(transactionID.getClientTransactionID())) {
        txnIDsToState.put(transactionID.getClientTransactionID(), new TransactionState(TransactionState.PASSIVE_START_STATE));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void nodeDead(CallBackOnComplete callBack) {
    this.callback = callBack;
    this.dead = true;
    invokeCallBackOnCompleteIfNecessary();
  }

  /**
   * Always returns first since {@link PassiveTransactionAccount} doesn't support waitee accounting.
   * 
   * @param TransactionID requestID
   * @return boolean
   */
  @Override
  public boolean hasWaitees(TransactionID requestID) {
    return false;
  }

  /**
   * @param NodeID waitee
   * @param TransactionID requestID
   * @throws AssertionError always, should not be called.
   */
  @Override
  public void addWaitee(NodeID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server : " + waitee + " , "
                             + requestID);
  }

  /**
   * @param TransactionID requestID
   * @return boolean
   * @throws AssertionError always, should not be called.
   */
  @Override
  public boolean broadcastCompleted(TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server");
  }

  /**
   * @param TransactionID requestID
   * @return boolean true when the TransactionState for the requestID is complete.
   */
  @Override
  public synchronized boolean processMetaDataCompleted(TransactionID requestID) {
    TransactionState state = txnIDsToState.get(requestID);
    state.processMetaDataCompleted();
    if (state.isComplete()) {
      this.txnIDsToState.remove(requestID);
      invokeCallBackOnCompleteIfNecessary();
      return true;
    }
    return false;
  }

  /**
   * @param TransactionID requestID
   * @return boolean
   * @throws AssertionError always, should not be called.
   */
  @Override
  public boolean removeWaitee(NodeID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be ACKED to Passive Server");
  }

  /**
   * @param TransactionID requestID
   * @return boolean
   * @throws AssertionError always, should not be called.
   */
  @Override
  public boolean relayTransactionComplete(TransactionID requestID) {
    throw new AssertionError("Transactions should never be relayed from Passive Server");
  }

  /**
   * Returns empty collection since {@link PassiveTransactionAccount} does not do waitee accounting.
   * 
   * @param NodeID waitee
   * @return Set set
   */
  @Override
  public Set<TransactionID> requestersWaitingFor(NodeID waitee) {
    return Collections.emptySet();
  }

  @Override
  public synchronized String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("PassiveTransactionAccount [ " + this.nodeID + " ] = \n");
    for (final Entry<TransactionID, TransactionState> entry : txnIDsToState.entrySet()) {
      stringBuilder.append("{").append(entry.getKey()).append(": ");
      stringBuilder.append(entry.getValue()).append("}\n");
    }
    return stringBuilder.toString();
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (dead && txnIDsToState.isEmpty()) {
      callback.onComplete(nodeID);
    }
  }

}
