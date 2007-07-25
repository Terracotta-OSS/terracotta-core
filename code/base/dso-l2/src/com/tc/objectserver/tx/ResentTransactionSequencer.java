/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * The whole purpose of this class is to reorder "resent" transactions so that they are broadcased in the exact same
 * order as before (the server crash)
 */
public class ResentTransactionSequencer implements ServerTransactionListener {

  private static final TCLogger                logger                 = TCLogging
                                                                          .getLogger(ResentTransactionSequencer.class);

  private static final State                   PASS_THRU              = new State("PASS_THRU");
  private static final State                   ADD_RESENT             = new State("ADD_RESENT");
  private static final State                   INCOMING_RESENT        = new State("INCOMING_RESENT");

  private final TransactionalObjectManager     txnObjectManager;
  private final ServerTransactionManager       transactionManager;
  private final ServerGlobalTransactionManager gtxm;
  private final List                           resentTxns             = new LinkedList();
  private final Map                            pendingTxns            = new LinkedHashMap();
  private final Collection                     pendingCompletedTxnIDs = new ArrayList();

  private State                                state                  = PASS_THRU;

  public ResentTransactionSequencer(ServerTransactionManager transactionManager, ServerGlobalTransactionManager gtxm,
                                    TransactionalObjectManager txnObjectManager) {
    this.transactionManager = transactionManager;
    this.gtxm = gtxm;
    this.txnObjectManager = txnObjectManager;
  }

  public synchronized void addTransactions(Collection txns, Collection completedTxnIds) {
    State lstate = state;
    if (lstate == PASS_THRU) {
      txnObjectManager.addTransactions(txns, completedTxnIds);
    } else if (lstate == INCOMING_RESENT) {
      addToPending(txns, completedTxnIds);
      processResent();
    } else {
      throw new AssertionError("Illegal State : " + state + " resentTxns : " + resentTxns);
    }
  }

  private synchronized void processResent() {
    ArrayList txns2Process = new ArrayList();
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = (TransactionDesc) i.next();
      ServerTransaction txn = (ServerTransaction) pendingTxns.remove(desc.getServerTransactionID());
      if (txn != null) {
        txns2Process.add(txn);
        i.remove();
      } else {
        break;
      }
    }
    if (!txns2Process.isEmpty()) {
      txnObjectManager.addTransactions(txns2Process, Collections.EMPTY_LIST);
    }
    moveToPassThruIfPossible();
  }

  private synchronized void addToPending(Collection txns, Collection completedTxnIds) {
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      pendingTxns.put(txn.getServerTransactionID(), txn);
    }
    pendingCompletedTxnIDs.addAll(completedTxnIds);
  }

  public synchronized void goToActiveMode() {
    this.transactionManager.addTransactionListener(this);
    this.state = ADD_RESENT;
  }

  public synchronized void transactionManagerStarted(Set cids) {
    this.state = INCOMING_RESENT;
    removeAllExceptFrom(cids);
    moveToPassThruIfPossible();
  }

  private void removeAllExceptFrom(Set cids) {
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = (TransactionDesc) i.next();
      if (!cids.contains(desc.getServerTransactionID().getChannelID())) {
        logger.warn("Removing " + desc + " because not in startup set " + cids);
        i.remove();
      }
    }
  }

  private void moveToPassThruIfPossible() {
    if (resentTxns.isEmpty()) {
      this.state = PASS_THRU;
      clearPending();
      logger.info("Unregistering ResentTransactionSequencer since no more resent Transactions : " + resentTxns.size());
      this.transactionManager.removeTransactionListener(this);
    }
  }

  private void clearPending() {
    txnObjectManager.addTransactions(pendingTxns.values(), pendingCompletedTxnIDs);
    pendingCompletedTxnIDs.clear();
    pendingTxns.clear();
  }

  public synchronized void addResentServerTransactionIDs(Collection stxIDs) {
    Assert.assertEquals(ADD_RESENT, state);
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) i.next();
      GlobalTransactionID gid = gtxm.getGlobalTransactionID(stxID);
      if (!gid.isNull()) {
        addOrdered(stxID, gid);
      }
    }
    assertGidsInOrder();
    logger.info("Resent Txns = " + resentTxns);
  }

  private void assertGidsInOrder() {
    long last = Long.MIN_VALUE;
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = (TransactionDesc) i.next();
      long current = desc.getGlobalTransactionID().toLong();
      if (current < last) { throw new AssertionError("Resent TransactionSequence Ordering error : " + resentTxns); }
      last = current;
    }
  }

  private void addOrdered(ServerTransactionID stxID, GlobalTransactionID gid) {
    TransactionDesc toAdd = new TransactionDesc(stxID, gid);
    ListIterator i;
    // Going from the reverse means less iterations
    for (i = resentTxns.listIterator(resentTxns.size()); i.hasPrevious();) {
      TransactionDesc desc = (TransactionDesc) i.previous();
      if (desc.getGlobalTransactionID().lessThan(toAdd.getGlobalTransactionID())) {
        i.next(); // move to the right position
        break;
      }
    }
    i.add(toAdd);
  }

  public synchronized void clearAllTransactionsFor(ChannelID killedClient) {
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = (TransactionDesc) i.next();
      if (desc.getServerTransactionID().getChannelID().equals(killedClient)) {
        logger.warn("Removing " + desc + " because " + killedClient + " is dead");
        i.remove();
      }
    }
    moveToPassThruIfPossible();
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    return;
  }

  public void transactionApplied(ServerTransactionID stxID) {
    return;
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    return;
  }

  private final static class TransactionDesc {

    private final ServerTransactionID stxID;
    private final GlobalTransactionID gid;

    public TransactionDesc(ServerTransactionID stxID, GlobalTransactionID gid) {
      this.stxID = stxID;
      this.gid = gid;
    }

    public GlobalTransactionID getGlobalTransactionID() {
      return gid;
    }

    public ServerTransactionID getServerTransactionID() {
      return stxID;
    }

    public String toString() {
      return "TxnDesc [" + gid + " , " + stxID + "]";
    }

  }

}
