/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
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
 * The whole purpose of this class is to reorder "resent" transactions so that they are broadcasted in the exact same
 * order as before (the server crash)
 * <p>
 * FIXME::This whole reordering of Resent transaction using GID only partially solves the problem. The reordering is
 * necessary because for object like LinkedBlockingQueue which uses 2 locks and some nasty optimizations, one node might
 * do a put() and that TXN might get broadcasted and then then on apply of that put, another node might do a get(). Now
 * the server might crash even before both the TXNs (put and the get) gets committed to disk. So when txns are resent,
 * the server has no way of telling which TXN to apply and broadcast first.
 * <p>
 * This class is an attempt to do that ordering but is still flawed in that it depends on the GID committed to disk so
 * only solves the case where at least the put() is committed to disk.
 * <p>
 * Two ideas to solve this right.
 * <p>
 * 1) On client handshake, re-send all the know GID to SID mapping to the server and the server can use all known info.
 * (Problem is with the TXNs that were never broadcasted, tx1 and tx3 is broadcasted but not tx2, how do you order them
 * <p>
 * 2) Commit GID to SID mapping first to disk before processing TXN, at least don't broadcast till its on disk. It adds
 * a little latency but on a heavy write system it should matter less.
 */
public class ResentTransactionSequencer extends AbstractServerTransactionListener {

  private static final TCLogger                logger            = TCLogging
                                                                     .getLogger(ResentTransactionSequencer.class);

  private static final State                   PASS_THRU_PASSIVE = new State("PASS_THRU_PASSIVE");
  private static final State                   PASS_THRU_ACTIVE  = new State("PASS_THRU_ACTIVE");
  private static final State                   ADD_RESENT        = new State("ADD_RESENT");
  private static final State                   INCOMING_RESENT   = new State("INCOMING_RESENT");

  private final TransactionalObjectManager     txnObjectManager;
  private final ServerTransactionManager       transactionManager;
  private final ServerGlobalTransactionManager gtxm;
  private final List                           resentTxns        = new LinkedList();
  private final Map                            pendingTxns       = new LinkedHashMap();
  private final List                           pendingCallBacks  = Collections.synchronizedList(new LinkedList());
  private State                                state             = PASS_THRU_PASSIVE;

  public ResentTransactionSequencer(ServerTransactionManager transactionManager, ServerGlobalTransactionManager gtxm,
                                    TransactionalObjectManager txnObjectManager) {
    this.transactionManager = transactionManager;
    this.gtxm = gtxm;
    this.txnObjectManager = txnObjectManager;
  }

  public void addTransactions(Collection txns) {
    boolean addPendingCallbacks = false;
    synchronized (this) {
      State lstate = state;
      if (lstate == PASS_THRU_ACTIVE || lstate == PASS_THRU_PASSIVE) {
        txnObjectManager.addTransactions(txns);
      } else if (lstate == INCOMING_RESENT) {
        addToPending(txns);
        addPendingCallbacks = processResent();
      } else {
        throw new AssertionError("Illegal State : " + state + " resentTxns : " + resentTxns);
      }
    }
    if (addPendingCallbacks) {
      addAndClearPendingCallBacks();
    }
  }

  public void callBackOnResentTxnsInSystemCompletion(TxnsInSystemCompletionLister l) {
    boolean addCallBack = false;
    synchronized (this) {
      if (state == PASS_THRU_ACTIVE) {
        addCallBack = true;
      } else {
        logger.info("Making callback " + l + " pending since in " + state + " resent txns size : " + resentTxns.size());
        pendingCallBacks.add(l);
      }
    }
    if (addCallBack) {
      // We can't be sure that the resent transactions are actually applied and committed already, so we wait for all
      // TXNs in the system to complete before calling back.
      transactionManager.callBackOnTxnsInSystemCompletion(l);
    }
  }

  private boolean processResent() {
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
      txnObjectManager.addTransactions(txns2Process);
    }
    return moveToPassThruActiveIfPossible();
  }

  private void addToPending(Collection txns) {
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      pendingTxns.put(txn.getServerTransactionID(), txn);
    }
  }

  public synchronized void goToActiveMode() {
    this.transactionManager.addTransactionListener(this);
    this.state = ADD_RESENT;
  }

  public void transactionManagerStarted(Set cids) {
    boolean addPendingCallbacks = false;
    synchronized (this) {
      this.state = INCOMING_RESENT;
      removeAllExceptFrom(cids);
      addPendingCallbacks = moveToPassThruActiveIfPossible();
    }
    if (addPendingCallbacks) {
      addAndClearPendingCallBacks();
    }
  }

  private void removeAllExceptFrom(Set cids) {
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = (TransactionDesc) i.next();
      if (!cids.contains(desc.getServerTransactionID().getSourceID())) {
        logger.warn("Removing " + desc + " because not in startup set " + cids);
        i.remove();
      }
    }
  }

  private boolean moveToPassThruActiveIfPossible() {
    if (resentTxns.isEmpty()) {
      this.state = PASS_THRU_ACTIVE;
      clearPending();
      logger.info("Unregistering ResentTransactionSequencer since no more resent Transactions : " + resentTxns.size());
      this.transactionManager.removeTransactionListener(this);
      return true;
    }
    return false;
  }

  private void clearPending() {
    txnObjectManager.addTransactions(pendingTxns.values());
    pendingTxns.clear();
  }

  public synchronized void addResentServerTransactionIDs(Collection stxIDs) {
    Assert.assertEquals(ADD_RESENT, state);
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) i.next();
      GlobalTransactionID gid = gtxm.getGlobalTransactionID(stxID);
      if (!gid.isNull()) {
        logger.info("Resent Transaction : " + stxID + " old gid = " + gid);
      } else {
        gid = gtxm.getOrCreateGlobalTransactionID(stxID);
        logger.info("Resent Transaction : " + stxID + " newly assigned gid = " + gid);
      }
      addOrdered(stxID, gid);
    }
    assertGidsInOrder();
    logger.info("Resent Txns = " + resentTxns);
  }

  private void assertGidsInOrder() {
    long last = Long.MIN_VALUE;
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = (TransactionDesc) i.next();
      long current = desc.getGlobalTransactionID().toLong();
      if (current <= last) { throw new AssertionError("Resent TransactionSequence Ordering error : " + resentTxns
                                                      + " current = " + current + " last = " + last); }
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

  public void clearAllTransactionsFor(NodeID deadNode) {
    boolean addPendingCallBacks;
    synchronized (this) {
      for (Iterator i = resentTxns.iterator(); i.hasNext();) {
        TransactionDesc desc = (TransactionDesc) i.next();
        if (desc.getServerTransactionID().getSourceID().equals(deadNode)) {
          logger.warn("Removing " + desc + " because " + deadNode + " is dead");
          i.remove();
        }
      }
      addPendingCallBacks = moveToPassThruActiveIfPossible();
    }
    if (addPendingCallBacks) {
      addAndClearPendingCallBacks();
    }
  }

  private void addAndClearPendingCallBacks() {
    TxnsInSystemCompletionLister[] pendingCallBacksCopy;
    synchronized (pendingCallBacks) {
      pendingCallBacksCopy = (TxnsInSystemCompletionLister[]) pendingCallBacks
          .toArray(new TxnsInSystemCompletionLister[pendingCallBacks.size()]);
      pendingCallBacks.clear();
    }
    for (int j = 0; j < pendingCallBacksCopy.length; j++) {
      logger.info("Adding Pending resent CallBacks to  TxnMgr : " + pendingCallBacksCopy[j]);
      transactionManager.callBackOnTxnsInSystemCompletion(pendingCallBacksCopy[j]);
    }
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
