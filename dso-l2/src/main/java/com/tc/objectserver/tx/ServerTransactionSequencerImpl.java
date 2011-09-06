/*
 * Copyright (c) 2003-2008 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.util.Assert;
import com.tc.util.MergableLinkedList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ServerTransactionSequencerImpl implements ServerTransactionSequencer, ServerTransactionSequencerStats {

  private static final TCLogger    logger      = TCLogging.getLogger(ServerTransactionSequencerImpl.class);

  private final Set                pendingTxns = new HashSet();

  private final MergableLinkedList txnQ        = new MergableLinkedList();
  private final MergableLinkedList blockedQ    = new MergableLinkedList();

  private final BlockedSet         objects     = new BlockedSet();

  private int                      txnsCount;
  private boolean                  reconcile   = false;

  public synchronized void addTransactionLookupContexts(Collection<TransactionLookupContext> txnLookupContexts) {
    if (false) log_incoming(txnLookupContexts);
    txnQ.addAll(txnLookupContexts);
    txnsCount += txnLookupContexts.size();
  }

  private void log_incoming(Collection lookupContexts) {
    for (Iterator i = lookupContexts.iterator(); i.hasNext();) {
      TransactionLookupContext lookupContext = (TransactionLookupContext) i.next();
      logger.info("Incoming : " + lookupContext);
    }
  }

  public synchronized TransactionLookupContext getNextTxnLookupContextToProcess() {
    reconcileIfNeeded();
    while (!txnQ.isEmpty()) {

      TransactionLookupContext lookupContext = (TransactionLookupContext) txnQ.removeFirst();
      ServerTransaction txn = lookupContext.getTransaction();
      if (isBlocked(txn)) {
        addBlocked(lookupContext);
      } else {
        if (false) log_outgoing(lookupContext);
        txnsCount--;
        return lookupContext;
      }
    }
    if (false) log_no_txns_to_process();
    return null;
  }

  private void reconcileIfNeeded() {
    if (reconcile) {
      // Add blockedQ to the beginning of txnQ, this call will also clear blockedQ
      txnQ.mergeToFront(blockedQ);
      objects.clearBlocked();
      reconcile = false;
    }
  }

  private void addBlocked(TransactionLookupContext lookupContext) {
    ServerTransaction txn = lookupContext.getTransaction();
    objects.addBlocked(txn.getObjectIDs());
    blockedQ.add(lookupContext);
  }

  private void log_no_txns_to_process() {
    if (txnsCount != 0) {
      int psize = pendingTxns.size();
      logger.info("No More Txns that can be processed : txnCount = " + txnsCount + " and pending txns = " + psize);
    }
  }

  private void log_outgoing(TransactionLookupContext lookupContext) {
    logger.info("Outgoing : " + lookupContext);
  }

  private boolean isBlocked(ServerTransaction txn) {
    return objects.isBlocked(txn.getObjectIDs());
  }

  public synchronized void makePending(ServerTransaction txn) {
    objects.makePending(txn.getObjectIDs());
    Assert.assertTrue(pendingTxns.add(txn.getServerTransactionID()));
    if (false) logger.info("Make Pending : " + txn);
  }

  public synchronized void makeUnpending(ServerTransaction txn) {
    Assert.assertTrue(pendingTxns.remove(txn.getServerTransactionID()));
    objects.makeUnpending(txn.getObjectIDs());
    reconcile = true;
    if (false) logger.info("Processed Pending : " + txn);
  }

  /*
   * Used for testing
   */
  synchronized boolean isPending(List txns) {
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction st = (ServerTransaction) i.next();
      if (pendingTxns.contains(st.getServerTransactionID())) return true;
    }
    return false;
  }

  
  /**
   * ===========================================================================
   * Stats Interface 
   * ===========================================================================
   */

  public synchronized int getBlockedObjectsCount() {
    return objects.getCount();
  }

  public synchronized int getBlockedTxnsCount() {
    return blockedQ.size();
  }

  public synchronized int getPendingTxnsCount() {
    return pendingTxns.size();
  }

  public synchronized int getTxnsCount() {
    return txnsCount;
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

    public int getCount() {
      return cause.size() + effect.size();
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

    public String toString() {
      StringBuffer toStringBuffer = new StringBuffer();
      toStringBuffer.append("cause: " + cause).append("\n").append("effect: " + effect).append("\n");
      return toStringBuffer.toString();
    }
  }
}
