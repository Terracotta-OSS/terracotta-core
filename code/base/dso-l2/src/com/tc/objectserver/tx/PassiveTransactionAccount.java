/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the transaction accounting on a passive server. It primaries keeps track of incoming transactions and those
 * transactions being applied on the passive server.
 * 
 * @author Saravanan Subbiah
 * @author Nabib El-Rahman
 */
public class PassiveTransactionAccount implements TransactionAccount {

  private final NodeID       nodeID;
  private final Set          txnIDs = Collections.synchronizedSet(new HashSet());
  private CallBackOnComplete callback;
  private boolean            dead   = false;

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
    synchronized (this.txnIDs) {
      this.txnIDs.remove(new ServerTransactionID(this.nodeID, requestID));
      invokeCallBackOnCompleteIfNecessary();
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public boolean skipApplyAndCommit(final TransactionID requestID) {
    synchronized (this.txnIDs) {
      this.txnIDs.remove(new ServerTransactionID(this.nodeID, requestID));
      invokeCallBackOnCompleteIfNecessary();
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public void addAllPendingServerTransactionIDsTo(Set<ServerTransactionID> txnsInSystem) {
    synchronized (txnIDs) {
      txnsInSystem.addAll(txnIDs);
    }
  }

  public void addObjectsSyncedTo(final NodeID to, final TransactionID txnID) {
    throw new AssertionError("Objects can't be synced from Passive Server");
  }

  /**
   * {@inheritDoc}
   */
  public void incomingTransactions(Set<ServerTransactionID> serverTxnsIDs) {
    txnIDs.addAll(serverTxnsIDs);
  }

  /**
   * {@inheritDoc}
   */
  public void nodeDead(CallBackOnComplete callBack) {
    synchronized (txnIDs) {
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
   * @return boolean
   * @throws AssertionError always, should not be called.
   */
  public boolean processMetaDataCompleted(TransactionID requestID) {
    throw new AssertionError("Transactions should never be processMetaData with a transaction in Passive Server");
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
    return "PassiveTransactionAccount [ " + this.nodeID + " ] = " + this.txnIDs;
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (dead && txnIDs.isEmpty()) {
      callback.onComplete(nodeID);
    }
  }

}
