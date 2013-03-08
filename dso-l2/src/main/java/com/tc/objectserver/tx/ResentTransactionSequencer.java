/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkState;

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
public class ResentTransactionSequencer extends AbstractServerTransactionListener implements PostInit {

  private static final TCLogger                logger            = TCLogging
                                                                     .getLogger(ResentTransactionSequencer.class);

  private enum State {
    PASS_THRU_PASSIVE, PASS_THRU_ACTIVE, ADD_RESENT, INCOMING_RESENT
  }

  private ServerTransactionManager             transactionManager;
  private ServerGlobalTransactionManager       gtxm;
  private ReplicatedObjectManager              replicatedObjectManager;

  private final SortedSet<TransactionDesc>     resentTxns        = new TreeSet<TransactionDesc>();
  private final Map<ServerTransactionID, TransactionBatchRecord> pendingBatches    = new LinkedHashMap<ServerTransactionID, TransactionBatchRecord>();
  private final List<TxnsInSystemCompletionListener>             pendingCallBacks  = Collections.synchronizedList(new LinkedList<TxnsInSystemCompletionListener>());
  private volatile State                                         state             = State.PASS_THRU_PASSIVE;

  @Override
  public void initializeContext(final ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext)context;
    transactionManager = scc.getTransactionManager();
    gtxm = scc.getServerGlobalTransactionManager();
    replicatedObjectManager = scc.getL2Coordinator().getReplicatedObjectManager();
  }

  public void addTransactions(TransactionBatchContext batchContext) {
    boolean addPendingCallbacks = false;
    TransactionBatchRecord record = new TransactionBatchRecord(batchContext);
    synchronized (this) {
      State lstate = this.state;
      switch (lstate) {
        case PASS_THRU_PASSIVE:
        case PASS_THRU_ACTIVE:
          record.process();
          break;
        case INCOMING_RESENT:
          addToPending(record);
          addPendingCallbacks = processResent();
          break;
        default:
          throw new IllegalStateException("Illegal State : " + lstate + " resentTxns : " + this.resentTxns);
      }
    }
    if (addPendingCallbacks) {
      addAndClearPendingCallBacks();
    }
  }

  public void callBackOnResentTxnsInSystemCompletion(TxnsInSystemCompletionListener l) {
    boolean addCallBack = false;
    synchronized (this) {
      if (state == State.PASS_THRU_ACTIVE) {
        addCallBack = true;
      } else {
        logger.info("Making callback " + l + " pending since in " + this.state + " resent txns size : "
                    + this.resentTxns.size());
        this.pendingCallBacks.add(l);
      }
    }
    if (addCallBack) {
      // We can't be sure that the resent transactions are actually applied and committed already, so we wait for all
      // TXNs in the system to complete before calling back.
      this.transactionManager.callBackOnTxnsInSystemCompletion(l);
    }
  }

  private boolean processResent() {
    for (Iterator<TransactionDesc> i = this.resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = i.next();
      TransactionBatchRecord batchRecord = pendingBatches.remove(desc.getServerTransactionID());
      if (batchRecord != null) {
        if (batchRecord.receivedTransaction(desc.getServerTransactionID())) {
          batchRecord.process();
        }
        i.remove();
      } else {
        break;
      }
    }
    return moveToPassThruActiveIfPossible();
  }

  private void addToPending(TransactionBatchRecord record) {
    for (ServerTransactionID serverTransactionID : record.getServerTransactionIDs()) {
      pendingBatches.put(serverTransactionID, record);
    }
  }

  public synchronized void goToActiveMode() {
    transactionManager.addTransactionListener(this);
    state = State.ADD_RESENT;
  }

  @Override
  public void transactionManagerStarted(Set cids) {
    boolean addPendingCallbacks;
    synchronized (this) {
      state = State.INCOMING_RESENT;
      removeAllExceptFrom(cids);
      addPendingCallbacks = moveToPassThruActiveIfPossible();
    }
    if (addPendingCallbacks) {
      addAndClearPendingCallBacks();
    }
  }

  private void removeAllExceptFrom(Set cids) {
    for (Iterator<TransactionDesc> i = this.resentTxns.iterator(); i.hasNext();) {
      TransactionDesc desc = i.next();
      if (!cids.contains(desc.getServerTransactionID().getSourceID())) {
        logger.warn("Removing " + desc + " because not in startup set " + cids);
        i.remove();
      }
    }
  }

  private boolean moveToPassThruActiveIfPossible() {
    if (state == State.INCOMING_RESENT && this.resentTxns.isEmpty()) {
      state = State.PASS_THRU_ACTIVE;
      clearPending();
      logger.info("Unregistering ResentTransactionSequencer since no more resent Transactions : "
                  + this.resentTxns.size());
      this.transactionManager.removeTransactionListener(this);
      return true;
    }
    return false;
  }

  private void clearPending() {
    for (TransactionBatchRecord record : new LinkedHashSet<TransactionBatchRecord>(pendingBatches.values())) {
      record.process();
    }
    pendingBatches.clear();
  }

  @Override
  public void addResentServerTransactionIDs(Collection stxIDs) {
    checkState(state == State.ADD_RESENT);

    Set<NodeID> clientIDs = new HashSet<NodeID>();
    int newlyAssigned = 0;
    for (final Object stxID1 : stxIDs) {
      ServerTransactionID stxID = (ServerTransactionID)stxID1;
      GlobalTransactionID gid = this.gtxm.getGlobalTransactionID(stxID);
      clientIDs.add(stxID.getSourceID());
      if (!gid.isNull()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Resent Transaction : " + stxID + " old gid = " + gid);
        }
      } else {
        gid = this.gtxm.getOrCreateGlobalTransactionID(stxID);
        if (logger.isDebugEnabled()) {
          logger.debug("Resent Transaction : " + stxID + " newly assigned gid = " + gid);
        }
        newlyAssigned++;
      }
      resentTxns.add(new TransactionDesc(stxID, gid));
    }
    logger.info("Resent Txns from " + clientIDs + " : Total number of resent txns = " + stxIDs.size()
                + " : Old already assigned gid = " + (stxIDs.size() - newlyAssigned) + " : Newly assigned gids = "
                + newlyAssigned);
    if (logger.isDebugEnabled()) {
      logger.debug("Total resent Txns so far = " + this.resentTxns);
    } else {
      logger.info("Total resent Txns so far = " + this.resentTxns.size());
    }
  }

  @Override
  public void clearAllTransactionsFor(NodeID deadNode) {
    boolean addPendingCallBacks;
    synchronized (this) {
      for (Iterator<TransactionDesc> i = this.resentTxns.iterator(); i.hasNext();) {
        TransactionDesc desc = i.next();
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
    TxnsInSystemCompletionListener[] pendingCallBacksCopy;
    synchronized (this.pendingCallBacks) {
      pendingCallBacksCopy = this.pendingCallBacks
          .toArray(new TxnsInSystemCompletionListener[this.pendingCallBacks.size()]);
      this.pendingCallBacks.clear();
    }
    for (TxnsInSystemCompletionListener element : pendingCallBacksCopy) {
      logger.info("Adding Pending resent CallBacks to  TxnMgr : " + element);
      this.transactionManager.callBackOnTxnsInSystemCompletion(element);
    }
  }

  private final static class TransactionDesc implements Comparable<TransactionDesc> {

    private final ServerTransactionID stxID;
    private final GlobalTransactionID gid;

    TransactionDesc(ServerTransactionID stxID, GlobalTransactionID gid) {
      this.stxID = stxID;
      this.gid = gid;
    }

    GlobalTransactionID getGlobalTransactionID() {
      return this.gid;
    }

    ServerTransactionID getServerTransactionID() {
      return this.stxID;
    }

    @Override
    public int compareTo(final TransactionDesc o) {
      return gid.compareTo(o.getGlobalTransactionID());
    }

    @Override
    public String toString() {
      return "TxnDesc [" + this.gid + " , " + this.stxID + "]";
    }
  }

  private class TransactionBatchRecord {
    private final Set<ServerTransactionID> serverTransactionIDs = new HashSet<ServerTransactionID>();
    private final TransactionBatchContext batchContext;

    TransactionBatchRecord(TransactionBatchContext batchContext) {
      this.batchContext = batchContext;
      serverTransactionIDs.addAll(batchContext.getTransactionIDs());
    }

    Set<ServerTransactionID> getServerTransactionIDs() {
      return serverTransactionIDs;
    }

    void process() {
      Map<ServerTransactionID, ServerTransaction> transactions = new LinkedHashMap<ServerTransactionID, ServerTransaction>(batchContext.getTransactions().size());
      for (ServerTransaction txn : batchContext.getTransactions()) {
        transactions.put(txn.getServerTransactionID(), txn);
      }
      transactionManager.incomingTransactions(batchContext.getSourceNodeID(), transactions);
      replicatedObjectManager.relayTransactions(batchContext);
    }

    boolean receivedTransaction(ServerTransactionID serverTransactionID) {
      if (!serverTransactionIDs.remove(serverTransactionID)) {
        throw new IllegalArgumentException("Transaction " + serverTransactionID + " is not part of this batch. " + serverTransactionIDs);
      }
      return serverTransactionIDs.isEmpty();
    }
  }
}
