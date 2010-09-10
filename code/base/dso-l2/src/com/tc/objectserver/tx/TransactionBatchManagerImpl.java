/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.l2.ha.TransactionBatchListener;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.SyncWriteTransactionReceivedContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.SequenceValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

public class TransactionBatchManagerImpl implements TransactionBatchManager, PostInit, PrettyPrintable {

  private static final TCLogger                logger       = TCLogging.getLogger(TransactionBatchManagerImpl.class);

  private final Map<NodeID, BatchStats>        map          = new HashMap<NodeID, BatchStats>();
  private final SequenceValidator              sequenceValidator;
  private final MessageRecycler                messageRecycler;
  private final Object                         lock         = new Object();

  private ServerTransactionManager             transactionManager;
  private ReplicatedObjectManager              replicatedObjectMgr;
  private Sink                                 txnRelaySink;
  private TransactionBatchReaderFactory        batchReaderFactory;
  private final TransactionFilter              filter;
  private ServerGlobalTransactionManager       gtxm;
  private DSOChannelManager                    dsoChannelManager;
  private final List<TransactionBatchListener> txnListeners = new CopyOnWriteArrayList<TransactionBatchListener>();
  private final Sink                           syncWriteTxnRecvdSink;

  public TransactionBatchManagerImpl(final SequenceValidator sequenceValidator, final MessageRecycler recycler,
                                     final TransactionFilter txnFilter, final Sink syncWriteTxnRecvdSink) {
    this.sequenceValidator = sequenceValidator;
    this.messageRecycler = recycler;
    this.filter = txnFilter;
    this.syncWriteTxnRecvdSink = syncWriteTxnRecvdSink;
  }

  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    this.transactionManager = oscc.getTransactionManager();
    this.replicatedObjectMgr = oscc.getL2Coordinator().getReplicatedObjectManager();
    this.gtxm = oscc.getServerGlobalTransactionManager();
    this.dsoChannelManager = oscc.getChannelManager();
    final Stage relayStage = oscc.getStage(ServerConfigurationContext.TRANSACTION_RELAY_STAGE);
    if (relayStage != null) {
      this.txnRelaySink = relayStage.getSink();
    }
  }

  public void addTransactionBatch(final CommitTransactionMessage ctm) {
    fireBatchTxnEvent(ctm);
    try {
      final TransactionBatchReader reader = this.batchReaderFactory.newTransactionBatchReader(ctm);

      ServerTransaction txn;

      // Transactions should maintain order.
      final ArrayList<ServerTransaction> txns = new ArrayList<ServerTransaction>(reader.getNumberForTxns());
      final HashSet<ServerTransactionID> txnIDs = new HashSet(reader.getNumberForTxns());
      final NodeID nodeID = reader.getNodeID();
      final HashSet<ObjectID> newObjectIDs = new HashSet<ObjectID>();
      final HashSet<TransactionID> syncWriteTxns = new HashSet<TransactionID>();

      while ((txn = reader.getNextTransaction()) != null) {
        this.sequenceValidator.setCurrent(nodeID, txn.getClientSequenceID());
        txns.add(txn);
        txnIDs.add(txn.getServerTransactionID());
        newObjectIDs.addAll(txn.getNewObjectIDs());
        if (txn.getTransactionType().equals(TxnType.SYNC_WRITE)) {
          syncWriteTxns.add(txn.getTransactionID());
        }
      }

      if (reader.containsSyncWriteTransaction()) {
        this.syncWriteTxnRecvdSink.add(new SyncWriteTransactionReceivedContext(reader.getBatchID().toLong(),
                                                                               (ClientID) ctm.getSourceNodeID(),
                                                                               syncWriteTxns));
      }

      defineBatch(nodeID, txns.size());
      this.messageRecycler.addMessage(ctm, txnIDs);

      this.filter.addTransactionBatch(new IncomingTransactionBatchContext(nodeID, txnIDs, reader, txns, newObjectIDs));
    } catch (final Exception e) {
      logger.error("Error reading transaction batch. : ", e);
      final MessageChannel c = ctm.getChannel();
      logger.error("Closing channel " + c.getChannelID() + " due to previous errors !");
      c.close();
    }
  }

  public void processTransactions(final TransactionBatchContext batchContext) {
    final List<ServerTransaction> txns = batchContext.getTransactions();
    final NodeID nodeID = batchContext.getSourceNodeID();
    // This lock is used to make sure the order in which we assign GIDs is the order in which we process transactions.
    // Even though this stage is single threaded today we use this lock to just to be sure.
    synchronized (this.lock) {
      try {
        /*
         * NOTE:: GlobalTransactionID id assigned in the process transaction stage. The transaction could be re-ordered
         * before apply. This is not a problem because for an transaction to be re-ordered, it should not have any
         * common objects between them. hence if g1 is the first TXN and g2 is the second TXN, g2 will be applied before
         * g1, only when g2 has no common objects with g1. If this is not true then we can't assign GID here.
         */
        for (final ServerTransaction txn : txns) {
          txn.setGlobalTransactionID(this.gtxm.getOrCreateGlobalTransactionID(txn.getServerTransactionID()));
        }
        if (this.replicatedObjectMgr.relayTransactions()) {
          this.transactionManager.incomingTransactions(nodeID, batchContext.getTransactionIDs(), txns, true);
          this.txnRelaySink.add(batchContext);
        } else {
          this.transactionManager.incomingTransactions(nodeID, batchContext.getTransactionIDs(), txns, false);
        }
      } catch (final Exception e) {
        logger.error("Error reading transaction batch. : ", e);
        logger.error("Closing channel " + nodeID + " due to previous errors !");
        this.dsoChannelManager.closeAll(Collections.singletonList(nodeID));
      }
    }
  }

  public synchronized void defineBatch(final NodeID nid, final int numTxns) {
    final BatchStats batchStats = getOrCreateStats(nid);
    batchStats.defineBatch(numTxns);
  }

  private BatchStats getOrCreateStats(final NodeID nid) {
    BatchStats bs = this.map.get(nid);
    if (bs == null) {
      bs = new BatchStats(nid);
      this.map.put(nid, bs);
    }
    return bs;
  }

  public synchronized boolean batchComponentComplete(final NodeID nid, final TransactionID txnID) {
    final BatchStats bs = this.map.get(nid);
    Assert.assertNotNull(bs);
    return bs.batchComplete(txnID);
  }

  public void nodeConnected(final NodeID nodeID) {
    this.transactionManager.nodeConnected(nodeID);
  }

  public void notifyServerHighWaterMark(final NodeID nodeID, final long serverHighWaterMark) {
    this.filter.notifyServerHighWaterMark(nodeID, serverHighWaterMark);
  }

  public void shutdownNode(final NodeID nodeID) {
    if (this.filter.shutdownNode(nodeID)) {
      shutdownBatchStats(nodeID);
      this.transactionManager.shutdownNode(nodeID);
    } else {
      logger.warn("Not clearing shutdownNode : " + nodeID);

    }
  }

  private synchronized void shutdownBatchStats(final NodeID nodeID) {
    final BatchStats bs = this.map.get(nodeID);
    if (bs != null) {
      bs.shutdownNode();
    }
  }

  private void cleanUp(final NodeID nodeID) {
    this.map.remove(nodeID);
  }

  public class BatchStats {
    private final NodeID nodeID;

    private int          batchCount;
    private int          txnCount;
    private float        avg;

    private boolean      killed = false;

    public BatchStats(final NodeID nid) {
      this.nodeID = nid;
    }

    public void defineBatch(final int numTxns) {
      final long adjustedTotal = (long) (this.batchCount * this.avg) + numTxns;
      this.txnCount += numTxns;
      this.batchCount++;
      this.avg = adjustedTotal / this.batchCount;
      if (false) {
        log_stats();
      }
    }

    private void log_stats() {
      logger.info(this);
    }

    @Override
    public String toString() {
      return "BatchStats : " + this.nodeID + " : batch count = " + this.batchCount + " txnCount = " + this.txnCount
             + " avg = " + this.avg;
    }

    private void log_stats(final float thresh) {
      logger.info(this + " threshold = " + thresh);
    }

    public boolean batchComplete(final TransactionID txnID) {
      if (this.txnCount <= 0) {
        // this is possible when the passive server moves to active.
        logger.info("Not decrementing txnCount : " + txnID + " : " + toString());
      } else {
        this.txnCount--;
      }
      if (this.killed) {
        // return true only when all TXNs are ACKed. Note new batches may still be in network read queue
        if (this.txnCount == 0) {
          cleanUp(this.nodeID);
          return true;
        } else {
          return false;
        }
      }
      final float threshold = (this.avg * (this.batchCount - 1));

      if (false) {
        log_stats(threshold);
      }

      if (this.txnCount <= threshold) {
        if (this.batchCount <= 0) {
          // this is possible when the passive server moves to active.
          logger.info("Not decrementing batchCount : " + txnID + " : " + toString());
        } else {
          this.batchCount--;
        }
        return true;
      } else {
        return false;
      }
    }

    public void shutdownNode() {
      this.killed = true;
      if (this.txnCount == 0) {
        cleanUp(this.nodeID);
      }
    }
  }

  public void registerForBatchTransaction(final TransactionBatchListener listener) {
    this.txnListeners.add(listener);
  }

  private void fireBatchTxnEvent(final CommitTransactionMessage ctm) {
    for (final TransactionBatchListener listener : this.txnListeners) {
      listener.notifyTransactionBatchAdded(ctm);
    }
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.print("BatchStats: " + this.map.size()).flush();
    for (final Entry<NodeID, BatchStats> e : this.map.entrySet()) {
      out.duplicateAndIndent().indent().print(e.getKey() + " => " + e.getValue()).flush();
    }
    return out;
  }
}
