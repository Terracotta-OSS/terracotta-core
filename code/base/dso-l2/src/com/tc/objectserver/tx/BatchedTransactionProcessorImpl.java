/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.BatchedTransactionProcessingContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.util.State;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BatchedTransactionProcessorImpl implements BatchedTransactionProcessor {

  private static final State                   OUT_OF_SINK         = new State("OUT_OF_SINK");
  private static final State                   IN_SINK             = new State("IN_SINK");

  private final Object                         completedTxnIdsLock = new Object();
  private final TransactionSequencer           sequencer;

  private volatile State                       state               = OUT_OF_SINK;
  private BatchedTransactionProcessingContext  batchedContext      = new BatchedTransactionProcessingContext();
  private Set                                  completedTxnIDs     = new HashSet();

  private final ServerGlobalTransactionManager gtxm;
  private final TransactionObjectManagerImpl   objectManager;
  private final Sink                           batchTxnLookupSink;

  public BatchedTransactionProcessorImpl(ObjectManager objectManager, ServerGlobalTransactionManager gtxm,
                                         Sink batchTxnLookupSink) {
    // TODO::pass this in
    this.sequencer = new TransactionSequencer();
    this.gtxm = gtxm;
    this.batchTxnLookupSink = batchTxnLookupSink;
    this.objectManager = new TransactionObjectManagerImpl(objectManager, sequencer, gtxm, batchTxnLookupSink);
  }

  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds) {
    sequencer.addTransactions(txns);
    addCompletedTxnIds(completedTxnIds);
    if (state != IN_SINK) addToSink();
  }

  public void processTransactions(EventContext context, Sink applyChangesSink) {
    state = OUT_OF_SINK;
    try {
      ServerTransaction txn;
      while ((txn = getNextTxn()) != null) {
        ServerTransactionID stxID = txn.getServerTransactionID();
        if (gtxm.needsApply(stxID)) {
          // objectManager.lookupObjectsForApply(txn);
        } else {
          // These txns are already applied, hence just sending it to the next stage.
          addTransactionToBatchedContext(txn);
        }
      }
    } finally {
      closeAndSendBatchedContext(applyChangesSink);
    }
  }

  private synchronized void closeAndSendBatchedContext(Sink applyChangesSink) {
    if (!batchedContext.isEmpty()) {
      batchedContext.close(getCompletedTxnIds());
      applyChangesSink.add(batchedContext);
      batchedContext = new BatchedTransactionProcessingContext();
    }
  }

  private synchronized void addTransactionToBatchedContext(ServerTransaction txn) {
    batchedContext.addTransaction(txn);
  }

  public synchronized Set getCheckedOutObjectIDs() {
    return batchedContext.getObjectIDs();
  }

  public synchronized void addLookedUpTransaction(ServerTransaction txn, Collection ids,
                                                  ObjectManagerLookupResults results, boolean pending) {
    addTransactionToBatchedContext(txn);
    batchedContext.addLookedUpObjects(ids, results.getObjects());
    if (pending) {
      // This was a pending request
      sequencer.processedPendingTxn(txn);
      if (state != IN_SINK) addToSink();
    }
  }

  private synchronized void addToSink() {
    state = IN_SINK;
    this.batchTxnLookupSink.add(new BatchedTxnLookupContext());
  }

  private ServerTransaction getNextTxn() {
    return sequencer.getNextTxnToProcess();
  }

  private void addCompletedTxnIds(Collection txnIds) {
    synchronized (completedTxnIdsLock) {
      completedTxnIDs.addAll(txnIds);
    }
  }

  private Set getCompletedTxnIds() {
    synchronized (completedTxnIdsLock) {
      Set toRet = completedTxnIDs;
      completedTxnIDs = new HashSet();
      return toRet;
    }
  }

  public synchronized void makePending(ServerTransaction txn) {
    sequencer.makePending(txn);
  }

  /*
   * This if for testing only
   */
  boolean isPending(List txns) {
    return sequencer.isPending(txns);
  }

  public synchronized String toString() {
    return "BatchedTransactionProcessorImpl = { " + state + " batchedContext = " + batchedContext + " } ";
  }

  public class TxnLookupContext implements ObjectManagerResultsContext {

    private final ServerTransaction txn;
    private volatile boolean        pending = false;

    public TxnLookupContext(ServerTransaction txn) {
      this.txn = txn;
    }

    public Set getCheckedOutObjectIDs() {
      if (pending) {
        return Collections.EMPTY_SET;
      } else {
        return BatchedTransactionProcessorImpl.this.getCheckedOutObjectIDs();
      }
    }

    public boolean isPendingRequest() {
      return pending;
    }

    // Make pending could be called more than once !
    public void makePending(ChannelID channelID, Collection ids) {
      if (!pending) {
        pending = true;
        BatchedTransactionProcessorImpl.this.makePending(txn);
      }
    }

    public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
      BatchedTransactionProcessorImpl.this.addLookedUpTransaction(txn, ids, results, pending);
      pending = false;
    }
  }

  private static final class BatchedTxnLookupContext implements EventContext {
    // this is just a dumb class used to indicate to the stage thread to start processing
  }

}
