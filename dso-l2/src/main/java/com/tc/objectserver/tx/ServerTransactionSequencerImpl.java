/*
 * Copyright (c) 2003-2008 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.util.Assert;
import com.tc.util.MergableLinkedList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerTransactionSequencerImpl implements ServerTransactionSequencer {

  private static final TCLogger    logger      = TCLogging.getLogger(ServerTransactionSequencerImpl.class);

  private final Set<ServerTransactionID>                pendingTxns = new HashSet<ServerTransactionID>();

  private final MergableLinkedList txnQ        = new MergableLinkedList();
  private final MergableLinkedList blockedQ    = new MergableLinkedList();

  private final BlockedSet         objects     = new BlockedSet();

  private int                      txnsCount;
  private boolean                  reconcile   = false;

  private final ServerTransactionSequencerStats tsStats     = new ServerTransactionSequencerStats() {

                                                              @Override
                                                              public int getBlockedObjectsCount() {
                                                                synchronized (ServerTransactionSequencerImpl.this) {
                                                                  return objects.getCount();
                                                                }
                                                              }

                                                              @Override
                                                              public int getBlockedTxnsCount() {
                                                                synchronized (ServerTransactionSequencerImpl.this) {
                                                                  return blockedQ.size();
                                                                }
                                                              }

                                                              @Override
                                                              public int getPendingTxnsCount() {
                                                                synchronized (ServerTransactionSequencerImpl.this) {
                                                                  return pendingTxns.size();
                                                                }
                                                              }

                                                              @Override
                                                              public int getTxnsCount() {
                                                                synchronized (ServerTransactionSequencerImpl.this) {
                                                                  return txnsCount;
                                                                }
                                                              }

                                                            };

  @Override
  public synchronized void addTransactionLookupContexts(Collection<TransactionLookupContext> txnLookupContexts) {
    if (false) log_incoming(txnLookupContexts);
    txnQ.addAll(txnLookupContexts);
    txnsCount += txnLookupContexts.size();
  }

  private void log_incoming(Collection<TransactionLookupContext> lookupContexts) {
    for (TransactionLookupContext lookupContext : lookupContexts) {
      logger.info("Incoming : " + lookupContext);
    }
  }

  @Override
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

  @Override
  public ServerTransactionSequencerStats getStats() {
    return tsStats;
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

  @Override
  public synchronized void makePending(ServerTransaction txn) {
    objects.makePending(txn.getObjectIDs());
    Assert.assertTrue(pendingTxns.add(txn.getServerTransactionID()));
    if (false) logger.info("Make Pending : " + txn);
  }

  @Override
  public synchronized void makeUnpending(ServerTransaction txn) {
    Assert.assertTrue(pendingTxns.remove(txn.getServerTransactionID()));
    objects.makeUnpending(txn.getObjectIDs());
    reconcile = true;
    if (false) logger.info("Processed Pending : " + txn);
  }

  /*
   * Used for testing
   */
  synchronized boolean isPending(List<ServerTransaction> txns) {
    for (ServerTransaction st : txns) {
      if (pendingTxns.contains(st.getServerTransactionID())) return true;
    }
    return false;
  }

  private static final class BlockedSet {

    Set<ObjectID> cause  = new HashSet<ObjectID>();
    Set<ObjectID> effect = new HashSet<ObjectID>();

    public boolean isBlocked(Collection<ObjectID> keys) {
      for (ObjectID o : keys) {
        if (cause.contains(o) || effect.contains(o)) { return true; }
      }
      return false;
    }

    public int getCount() {
      return cause.size() + effect.size();
    }

    public void makePending(Collection<ObjectID> keys) {
      cause.addAll(keys);
    }

    public void makeUnpending(Collection<ObjectID> keys) {
      cause.removeAll(keys);
    }

    public void addBlocked(Collection<ObjectID> keys) {
      effect.addAll(keys);
    }

    public void clearBlocked() {
      effect.clear();
    }

    @Override
    public String toString() {
      StringBuffer toStringBuffer = new StringBuffer();
      toStringBuffer.append("cause: " + cause).append("\n").append("effect: " + effect).append("\n");
      return toStringBuffer.toString();
    }
  }
}
