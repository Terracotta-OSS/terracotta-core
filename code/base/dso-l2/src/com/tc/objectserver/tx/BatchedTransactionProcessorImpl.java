/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.BatchedTransactionProcessingContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.util.Assert;
import com.tc.util.SequenceID;
import com.tc.util.SequenceValidator;
import com.tc.util.State;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BatchedTransactionProcessorImpl implements BatchedTransactionProcessor {

  private static final TCLogger                logger                     = TCLogging
                                                                              .getLogger(BatchedTransactionProcessorImpl.class);

  private static final int                     MAX_TRANSACTIONS_PER_BATCH = 1000;

  private final Object                         create_lock                = new Object();
  private final int                            maxDepth;
  private final Map                            channel2Queue              = new ConcurrentReaderHashMap();
  private final ServerGlobalTransactionManager gtxm;
  private final ObjectManager                  objectManager;
  private final SequenceValidator              sequenceValidator;
  private final Sink                           batchTxnLookupSink;

  public BatchedTransactionProcessorImpl(int maxDepth, SequenceValidator sequenceValidator,
                                         ObjectManager objectManager, ServerGlobalTransactionManager gtxm,
                                         Sink batchTxnLookupSink) {
    this.maxDepth = maxDepth;
    this.sequenceValidator = sequenceValidator;
    this.objectManager = objectManager;
    this.gtxm = gtxm;
    this.batchTxnLookupSink = batchTxnLookupSink;
  }

  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds) {
    TransactionQueue txnQ = getOrCreateTxQueueFor(channelID);
    txnQ.addTransactions(txns, completedTxnIds);
  }

  public void processTransactions(EventContext context, Sink applyChangesSink) {
    TransactionQueue txnQ = (TransactionQueue) context;
    txnQ.processTransactions(applyChangesSink);
  }

  /*
   * the method blocks till the transactions are batched.
   */
  public void shutDownClient(ChannelID channelID) {
    TransactionQueue q = (TransactionQueue) channel2Queue.get(channelID);
    if (q != null) {
      try {
        q.waitTillEmpty();
      } catch (InterruptedException e) {
        logger.error(e);
      }
      // XXX:: There is a race here the queue could momentarily become empty.
      channel2Queue.remove(channelID);
    }
  }

  // this is for testing purpose only !!
  boolean isPending(ChannelID channelID) {
    TransactionQueue txnQ = getOrCreateTxQueueFor(channelID);
    return txnQ.isPendingRequest();
  }

  private TransactionQueue getOrCreateTxQueueFor(ChannelID channelID) {
    TransactionQueue q = (TransactionQueue) channel2Queue.get(channelID);
    if (q == null) {
      synchronized (create_lock) {
        q = (TransactionQueue) channel2Queue.get(channelID);
        if (q == null) {
          q = createAQueue(channelID);
        }
        channel2Queue.put(channelID, q);
      }
    }
    return q;
  }

  private TransactionQueue createAQueue(ChannelID channelID) {
    return new TransactionQueue(maxDepth, channelID, sequenceValidator, gtxm, objectManager, batchTxnLookupSink);
  }

  private static final class TransactionQueue implements ObjectManagerResultsContext {

    private static final State                           PENDING             = new State("PENDING");
    private static final State                           PROCESSING          = new State("PROCESSING");
    private static final State                           READY               = new State("READY");
    private static final State                           IN_SYNC             = new State("IN_SYNC");

    private final Channel                                queue;
    private final ChannelID                              channelID;
    private final ObjectManager                          objectManager;
    private final SequenceValidator                      sequenceValidator;
    private final ServerGlobalTransactionManager         gtxm;
    private final Sink                                   batchTxnLookupSink;
    private final Object                                 completedTxnIdsLock = new Object();

    private volatile State                               state               = READY;
    private volatile BatchedTransactionProcessingContext batchedContext      = new BatchedTransactionProcessingContext();
    private volatile ServerTransaction                   currentTxn;
    private Set                                          completedTxnIDs     = new HashSet();

    public TransactionQueue(int maxDepth, ChannelID channelID, SequenceValidator sequenceValidator,
                            ServerGlobalTransactionManager gtxm, ObjectManager objectManager, Sink batchTxnLookupSink) {
      this.channelID = channelID;
      this.sequenceValidator = sequenceValidator;
      this.gtxm = gtxm;
      this.objectManager = objectManager;
      this.batchTxnLookupSink = batchTxnLookupSink;
      queue = maxDepth > 0 ? (Channel) new BoundedLinkedQueue(maxDepth) : new LinkedQueue();
    }

    // Channel doesnt have isEmpty() on it.
    private boolean isEmpty() {
      if (queue instanceof LinkedQueue) {
        return ((LinkedQueue) queue).isEmpty();
      } else {
        return ((BoundedLinkedQueue) queue).isEmpty();
      }
    }

    public synchronized void waitTillEmpty() throws InterruptedException {
      while (!isEmpty()) {
        wait();
      }
    }

    public synchronized void processTransactions(Sink applyChangesSink) {
      state = PROCESSING;
      int count = 0;
      try {
        while ((count++ <= MAX_TRANSACTIONS_PER_BATCH) && (currentTxn = getNextTxn()) != null) {
          ServerTransaction txn = this.currentTxn;
          ServerTransactionID stxID = txn.getServerTransactionID();
          SequenceID sequenceID = txn.getClientSequenceID();
          if (gtxm.needsApply(stxID)) {
            // Currently transactions should never be send out of order. If that changes then our usage of the queue
            // should change to a sortedlist based on SequenceIDs
            if (!sequenceValidator.isNext(channelID, sequenceID)) {
              // Formatter
              throw new AssertionError("SequenceID Validator Failed : current = "
                                       + sequenceValidator.getCurrent(channelID) + " but this = " + sequenceID
                                       + "\n TransactionQueue = " + this);
            }
            if (!objectManager.lookupObjectsForCreateIfNecessary(txn.getChannelID(), txn.getObjectIDs(), this)) {
              // Going to pending
              return;
            }
          } else {
            logger.info(txn.getChannelID() + " : Resetting inital Sequence number to " + txn.getClientSequenceID());
            sequenceValidator.setCurrent(txn.getChannelID(), txn.getClientSequenceID());
            addTransactionToBatchedContext(txn);
          }
        }
      } finally {
        closeAndSendBatchedContext(applyChangesSink);
        if (state == PROCESSING) {
          state = READY;
          if (!isEmpty()) addToSink();
        }
        notifyAll();
      }
    }

    private void closeAndSendBatchedContext(Sink applyChangesSink) {
      if (!batchedContext.isEmpty()) {
        batchedContext.close(getCompletedTxnIds());
        // logger.info("Batching Transactions size () = " + batchedContext.getTransactionsCount());
        applyChangesSink.add(batchedContext);
        batchedContext = new BatchedTransactionProcessingContext();
      }
    }

    private void addTransactionToBatchedContext(ServerTransaction txn) {
      batchedContext.addTransaction(txn);
    }

    public synchronized Set getCheckedOutObjectIDs() {
      return batchedContext.getObjectIDs();
    }

    public synchronized void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
      Assert.assertEquals(channelID, chID);
      addTransactionToBatchedContext(currentTxn);
      batchedContext.addLookedUpObjects(ids, results.getObjects());
      sequenceValidator.setCurrent(channelID, currentTxn.getClientSequenceID());
      if (state == PENDING) {
        state = READY;
        addToSink();
      }
    }

    private synchronized void addToSink() {
      if (state != READY) return;
      state = IN_SYNC;
      this.batchTxnLookupSink.add(this);
    }

    private ServerTransaction getNextTxn() {
      try {
        return (ServerTransaction) queue.poll(0);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    public void addTransactions(List txns, Collection completedTxnIds) {

      try {
        for (Iterator it = txns.iterator(); it.hasNext();) {
          queue.put(it.next());
        }
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      addCompletedTxnIds(completedTxnIds);
      if (state == READY) addToSink();
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

    public boolean isPendingRequest() {
      return (state == PENDING);
    }

    public void makePending(ChannelID chID, Collection ids) {
      Assert.assertEquals(channelID, chID);
      state = PENDING;
    }

    public String toString() {
      return "TransactionQueue[" + channelID + " ] = { " + state + " current Txn = " + currentTxn
             + " batchedContext = " + batchedContext + " } ";
    }

  }

}
