/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestServerTransactionManager implements ServerTransactionManager {

  public Map                          txns      = new HashMap();
  public final NoExceptionLinkedQueue skipCalls = new NoExceptionLinkedQueue();

  public TestServerTransactionManager() {
    //
  }

  public final NoExceptionLinkedQueue shutdownClientCalls = new NoExceptionLinkedQueue();
  public final ArrayList incomingTxnContexts = new ArrayList();

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

  public void defineBatch(ChannelID channelID, TxnBatchID batchID, int count) {
    throw new ImplementMe();
  }

  public boolean startProgress(ChannelID channelID, TransactionID transactionID) {
    throw new ImplementMe();
  }

  public boolean needsApply(ChannelID channelID, TransactionID transactionID) {
    throw new ImplementMe();
  }

  public void completeTransactions(Collection collection) {
    throw new ImplementMe();

  }

  public void dump() {
    throw new ImplementMe();
  }

  public void apply(GlobalTransactionID gtxID, ServerTransaction txn, Map objects, BackReferences includeIDs) {
    this.txns.put(txn, objects);
  }

  public void release(PersistenceTransaction ptx, Set objects, Map newRoots) {
    throw new ImplementMe();
  }

  public void committed(Set txnset) {
    //NOP
  }

  public void broadcasted(ChannelID waiter, TransactionID requestID) {
    // NOP
  }

  public void skipApplyAndCommit(ServerTransaction txn) {
    skipCalls.put(txn);
  }

  public void setResentTransactionIDs(ChannelID channelID, Collection transactionIDs) {
    // NOP
  }

  public void addTransactionListener(ServerTransactionListener listener) {
    // NOP
  }

  public void apply(GlobalTransactionID gtxID, ServerTransaction txn, Map objects, BackReferences includeIDs,
                    ObjectInstanceMonitor instanceMonitor) {
    // NOP
  }

  public void committed(Collection tx) {
    //NOP
  }

  public void release(PersistenceTransaction ptx, Collection objects, Map newRoots) {
    throw new ImplementMe();
  }

  public void incomingTransactions(ChannelID channelID, Set serverTxnIDs, boolean relayed) {
    incomingTxnContexts.add(new Object[] { channelID, serverTxnIDs, Boolean.valueOf(relayed) } );
  }

  public void transactionsRelayed(ChannelID channelID, Set serverTxnIDs) {
    throw new ImplementMe();
  }

}
