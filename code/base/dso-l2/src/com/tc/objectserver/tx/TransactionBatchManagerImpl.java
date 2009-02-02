/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.l2.context.IncomingTransactionContext;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.util.Assert;
import com.tc.util.SequenceValidator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class TransactionBatchManagerImpl implements TransactionBatchManager, PostInit {

  private static final TCLogger          logger = TCLogging.getLogger(TransactionBatchManagerImpl.class);

  private final Map                      map    = new HashMap();

  private final SequenceValidator        sequenceValidator;
  private final MessageRecycler          messageRecycler;
  private final Object                   lock   = new Object();

  private ServerTransactionManager       transactionManager;
  private ReplicatedObjectManager        replicatedObjectMgr;
  private Sink                           txnRelaySink;
  private TransactionBatchReaderFactory  batchReaderFactory;
  private TransactionFilter              filter;
  private ServerGlobalTransactionManager gtxm;

  public TransactionBatchManagerImpl(SequenceValidator sequenceValidator, MessageRecycler recycler,
                                     TransactionFilter txnFilter) {
    this.sequenceValidator = sequenceValidator;
    this.messageRecycler = recycler;
    this.filter = txnFilter;
  }

  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    transactionManager = oscc.getTransactionManager();
    replicatedObjectMgr = oscc.getL2Coordinator().getReplicatedObjectManager();
    gtxm = oscc.getServerGlobalTransactionManager();
    Stage relayStage = oscc.getStage(ServerConfigurationContext.TRANSACTION_RELAY_STAGE);
    if (relayStage != null) {
      txnRelaySink = relayStage.getSink();
    }
  }

  public void addTransactionBatch(final CommitTransactionMessage ctm) {
    try {
      final TransactionBatchReader reader = batchReaderFactory.newTransactionBatchReader(ctm);

      ServerTransaction txn;

      // Transactions should maintain order.
      LinkedHashMap<ServerTransactionID, ServerTransaction> txns = new LinkedHashMap<ServerTransactionID, ServerTransaction>(
                                                                                                                             reader
                                                                                                                                 .getRemainingTxnsToBeRead());
      NodeID nodeID = reader.getNodeID();
      HashSet<ObjectID> newObjectIDs = new HashSet<ObjectID>();

      while ((txn = reader.getNextTransaction()) != null) {
        sequenceValidator.setCurrent(nodeID, txn.getClientSequenceID());
        txns.put(txn.getServerTransactionID(), txn);
        newObjectIDs.addAll(txn.getNewObjectIDs());
      }

      filter.addTransactionBatch(new IncomingTransactionBatchContext(nodeID, ctm, txns, reader.getHighWatermark(),
                                                                     newObjectIDs));
    } catch (Exception e) {
      logger.error("Error reading transaction batch. : ", e);
      MessageChannel c = ctm.getChannel();
      logger.error("Closing channel " + c.getChannelID() + " due to previous errors !");
      c.close();
    }
  }

  public void processTransactionBatch(TransactionBatchContext batchContext) {
    // This lock is used to make sure the order in which we assign GIDs is the order in which we process transactions.
    // Even though this stage is single threaded today we use this lock to just to be sure.
    synchronized (lock) {
      final CommitTransactionMessage ctm = batchContext.getCommitTransactionMessage();
      try {
        NodeID nodeID = batchContext.getSourceNodeID();
        defineBatch(nodeID, batchContext.getNumTxns());

        // Transactions should maintain order.
        LinkedHashMap<ServerTransactionID, ServerTransaction> txns = batchContext.getTransactions();

        /*
         * NOTE:: GlobalTransactionID id assigned in the process transaction stage. The transaction could be re-ordered
         * before apply. This is not a problem because for an transaction to be re-ordered, it should not have any
         * common objects between them. hence if g1 is the first TXN and g2 is the second TXN, g2 will be applied before
         * g1, only when g2 has no common objects with g1. If this is not true then we can't assign GID here.
         */
        for (Iterator<ServerTransaction> i = txns.values().iterator(); i.hasNext();) {
          ServerTransaction txn = i.next();
          txn.setGlobalTransactionID(gtxm.getOrCreateGlobalTransactionID(txn.getServerTransactionID()));
        }
        messageRecycler.addMessage(ctm, txns.keySet());
        if (replicatedObjectMgr.relayTransactions()) {
          transactionManager.incomingTransactions(nodeID, txns.keySet(), txns.values(), true);
          txnRelaySink.add(new IncomingTransactionContext(nodeID, ctm, txns));
        } else {
          transactionManager.incomingTransactions(nodeID, txns.keySet(), txns.values(), false);
        }
      } catch (Exception e) {
        logger.error("Error reading transaction batch. : ", e);
        MessageChannel c = ctm.getChannel();
        logger.error("Closing channel " + c.getChannelID() + " due to previous errors !");
        c.close();
      }
    }
  }

  public synchronized void defineBatch(NodeID nid, int numTxns) {
    BatchStats batchStats = getOrCreateStats(nid);
    batchStats.defineBatch(numTxns);
  }

  private BatchStats getOrCreateStats(NodeID nid) {
    BatchStats bs = (BatchStats) map.get(nid);
    if (bs == null) {
      bs = new BatchStats(nid);
      map.put(nid, bs);
    }
    return bs;
  }

  public synchronized boolean batchComponentComplete(NodeID nid, TransactionID txnID) {
    BatchStats bs = (BatchStats) map.get(nid);
    Assert.assertNotNull(bs);
    return bs.batchComplete(txnID);
  }

  public void nodeConnected(NodeID nodeID) {
    transactionManager.nodeConnected(nodeID);
  }

  public void shutdownNode(NodeID nodeID) {
    if (filter.shutdownNode(nodeID)) {
      shutdownBatchStats(nodeID);
      transactionManager.shutdownNode(nodeID);
    } else {
      logger.warn("Not clearing shutdownNode : " + nodeID);

    }
  }

  private synchronized void shutdownBatchStats(NodeID nodeID) {
    BatchStats bs = (BatchStats) map.get(nodeID);
    if (bs != null) {
      bs.shutdownNode();
    }
  }

  private void cleanUp(NodeID nodeID) {
    map.remove(nodeID);
  }

  public class BatchStats {
    private final NodeID nodeID;

    private int          batchCount;
    private int          txnCount;
    private float        avg;

    private boolean      killed = false;

    public BatchStats(NodeID nid) {
      this.nodeID = nid;
    }

    public void defineBatch(int numTxns) {
      long adjustedTotal = (long) (batchCount * avg) + numTxns;
      txnCount += numTxns;
      batchCount++;
      avg = adjustedTotal / batchCount;
      if (false) log_stats();
    }

    private void log_stats() {
      logger.info(this);
    }

    public String toString() {
      return "BatchStats : " + nodeID + " : batch count = " + batchCount + " txnCount = " + txnCount + " avg = " + avg;
    }

    private void log_stats(float thresh) {
      logger.info(this + " threshold = " + thresh);
    }

    public boolean batchComplete(TransactionID txnID) {
      if (txnCount <= 0) {
        // this is possible when the passive server moves to active.
        logger.info("Not decrementing txnCount : " + txnID + " : " + this.toString());
      } else {
        txnCount--;
      }
      if (killed) {
        // return true only when all TXNs are ACKed. Note new batches may still be in network read queue
        if (txnCount == 0) {
          cleanUp(nodeID);
          return true;
        } else {
          return false;
        }
      }
      float threshold = (avg * (batchCount - 1));

      if (false) log_stats(threshold);

      if (txnCount <= threshold) {
        if (batchCount <= 0) {
          // this is possible when the passive server moves to active.
          logger.info("Not decrementing batchCount : " + txnID + " : " + this.toString());
        } else {
          batchCount--;
        }
        return true;
      } else {
        return false;
      }
    }

    public void shutdownNode() {
      this.killed = true;
      if (txnCount == 0) {
        cleanUp(nodeID);
      }
    }
  }
}
