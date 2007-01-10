/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TransactionSequencer {

  private static final TCLogger logger      = TCLogging.getLogger(TransactionSequencer.class);

  private final Set             pendingTxns = new HashSet();

  private final LinkedList      txnQ        = new LinkedList();
  private final LinkedList      blockedQ    = new LinkedList();

  private final BlockedSet      locks       = new BlockedSet();
  private final BlockedSet      objects     = new BlockedSet();

  private int                   txnsCount;
  private boolean               reconcile   = false;

  public synchronized void addTransactions(List txns) {
    if (false) log_incoming(txns);
    txnQ.addAll(txns);
    txnsCount += txns.size();
  }

  private void log_incoming(List txns) {
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      logger.info("Incoming : " + txn);
    }
  }

  public synchronized ServerTransaction getNextTxnToProcess() {
    reconcileIfNeeded();
    while (!txnQ.isEmpty()) {

      ServerTransaction txn = (ServerTransaction) txnQ.removeFirst();
      if (isBlocked(txn)) {
        addBlocked(txn);
      } else {
        if (false) log_outgoing(txn);
        txnsCount--;
        return txn;
      }
    }
    if (false) log_no_txns_to_process();
    return null;
  }

  private void reconcileIfNeeded() {
    if (reconcile) {
      // Add to begining
      txnQ.addAll(0, blockedQ);
      blockedQ.clear();
      locks.clearBlocked();
      objects.clearBlocked();
      reconcile = false;
    }
  }

  private void addBlocked(ServerTransaction txn) {
    locks.addBlocked(Arrays.asList(txn.getLockIDs()));
    objects.addBlocked(txn.getObjectIDs());
    blockedQ.add(txn);
  }

  private void log_no_txns_to_process() {
    if (txnsCount != 0) {
      int psize = pendingTxns.size();
      logger.info("No More Txns that can be processed : txnCount = " + txnsCount + " and pending txns = " + psize);
    }
  }

  private void log_outgoing(ServerTransaction txn) {
    logger.info("Outgoing : " + txn);
  }

  private boolean isBlocked(ServerTransaction txn) {
    return locks.isBlocked(Arrays.asList(txn.getLockIDs())) || objects.isBlocked(txn.getObjectIDs());
  }

  public synchronized void makePending(ServerTransaction txn) {
    locks.makePending(Arrays.asList(txn.getLockIDs()));
    objects.makePending(txn.getObjectIDs());
    Assert.assertTrue(pendingTxns.add(txn.getServerTransactionID()));
    if (false) logger.info("Make Pending : " + txn);
  }

  public synchronized void makeUnpending(ServerTransaction txn) {
    Assert.assertTrue(pendingTxns.remove(txn.getServerTransactionID()));
    locks.makeUnpending(Arrays.asList(txn.getLockIDs()));
    objects.makeUnpending(txn.getObjectIDs());
    reconcile = true;
    if (false) logger.info("Processed Pending : " + txn);
  }

  /*
   * Used for testing
   */
  boolean isPending(List txns) {
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction st = (ServerTransaction) i.next();
      if (pendingTxns.contains(st.getServerTransactionID())) return true;
    }
    return false;
  }

  private static final class BlockedSet {

    Set cause  = new HashSet();
    Set effect = new HashSet();

    public boolean isBlocked(Collection keys) {
      for (Iterator i = keys.iterator(); i.hasNext();) {
        Object o = i.next();
        if (cause.contains(o) || effect.contains(o)) { return true; }
      }
      return false;
    }

    public void makePending(Collection keys) {
      cause.addAll(keys);
    }

    public void makeUnpending(Collection keys) {
      cause.removeAll(keys);
    }

    public void addBlocked(Collection keys) {
      effect.addAll(keys);
    }

    public void clearBlocked() {
      effect.clear();
    }
  }
}
