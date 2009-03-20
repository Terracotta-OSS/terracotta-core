/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.util.Assert;
import com.tc.util.SequenceValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
  private final TransactionFilter        filter;
  private ServerGlobalTransactionManager gtxm;
  private DSOChannelManager              dsoChannelManager;

  public TransactionBatchManagerImpl(SequenceValidator sequenceValidator, MessageRecycler recycler,
                                     TransactionFilter txnFilter) {
    this.sequenceValidator = sequenceValidator;
    this.messageRecycler = recycler;
    this.filter = txnFilter;
  }

  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    this.transactionManager = oscc.getTransactionManager();
    this.replicatedObjectMgr = oscc.getL2Coordinator().getReplicatedObjectManager();
    this.gtxm = oscc.getServerGlobalTransactionManager();
    this.dsoChannelManager = oscc.getChannelManager();
    Stage relayStage = oscc.getStage(ServerConfigurationContext.TRANSACTION_RELAY_STAGE);
    if (relayStage != null) {
      this.txnRelaySink = relayStage.getSink();
    }
  }

  public void addTransactionBatch(final CommitTransactionMessage ctm) {
    try {
      final TransactionBatchReader reader = this.batchReaderFactory.newTransactionBatchReader(ctm);

      ServerTransaction txn;

      // Transactions should maintain order.
      ArrayList<ServerTransaction> txns = new ArrayList<ServerTransaction>(reader.getNumberForTxns());
      HashSet<ServerTransactionID> txnIDs = new HashSet(reader.getNumberForTxns());
      NodeID nodeID = reader.getNodeID();
      HashSet<ObjectID> newObjectIDs = new HashSet<ObjectID>();

      while ((txn = reader.getNextTransaction()) != null) {
        this.sequenceValidator.setCurrent(nodeID, txn.getClientSequenceID());
        txns.add(txn);
        txnIDs.add(txn.getServerTransactionID());
        newObjectIDs.addAll(txn.getNewObjectIDs());
      }

      defineBatch(nodeID, txns.size());
      this.messageRecycler.addMessage(ctm, txnIDs);

      this.filter.addTransactionBatch(new IncomingTransactionBatchContext(nodeID, txnIDs, reader, txns, newObjectIDs));
    } catch (Exception e) {
      logger.error("Error reading transaction batch. : ", e);
      MessageChannel c = ctm.getChannel();
      logger.error("Closing channel " + c.getChannelID() + " due to previous errors !");
      c.close();
    }
  }

  public void processTransactions(TransactionBatchContext batchContext) {
    List<ServerTransaction> txns = batchContext.getTransactions();
    NodeID nodeID = batchContext.getSourceNodeID();
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
        for (ServerTransaction txn : txns) {
          txn.setGlobalTransactionID(this.gtxm.getOrCreateGlobalTransactionID(txn.getServerTransactionID()));
        }
        if (this.replicatedObjectMgr.relayTransactions()) {
          this.transactionManager.incomingTransactions(nodeID, batchContext.getTransactionIDs(), txns, true);
          this.txnRelaySink.add(batchContext);
        } else {
          this.transactionManager.incomingTransactions(nodeID, batchContext.getTransactionIDs(), txns, false);
        }
      } catch (Exception e) {
        logger.error("Error reading transaction batch. : ", e);
        logger.error("Closing channel " + nodeID + " due to previous errors !");
        this.dsoChannelManager.closeAll(Collections.singletonList(nodeID));
      }
    }
  }

  public synchronized void defineBatch(NodeID nid, int numTxns) {
    BatchStats batchStats = getOrCreateStats(nid);
    batchStats.defineBatch(numTxns);
  }

  private BatchStats getOrCreateStats(NodeID nid) {
    BatchStats bs = (BatchStats) this.map.get(nid);
    if (bs == null) {
      bs = new BatchStats(nid);
      this.map.put(nid, bs);
    }
    return bs;
  }

  public synchronized boolean batchComponentComplete(NodeID nid, TransactionID txnID) {
    BatchStats bs = (BatchStats) this.map.get(nid);
    Assert.assertNotNull(bs);
    return bs.batchComplete(txnID);
  }

  public void nodeConnected(NodeID nodeID) {
    this.transactionManager.nodeConnected(nodeID);
  }

  public void notifyServerHighWaterMark(NodeID nodeID, long serverHighWaterMark) {
    this.filter.notifyServerHighWaterMark(nodeID, serverHighWaterMark);
  }

  public void shutdownNode(NodeID nodeID) {
    if (this.filter.shutdownNode(nodeID)) {
      shutdownBatchStats(nodeID);
      this.transactionManager.shutdownNode(nodeID);
    } else {
      logger.warn("Not clearing shutdownNode : " + nodeID);

    }
  }

  private synchronized void shutdownBatchStats(NodeID nodeID) {
    BatchStats bs = (BatchStats) this.map.get(nodeID);
    if (bs != null) {
      bs.shutdownNode();
    }
  }

  private void cleanUp(NodeID nodeID) {
    this.map.remove(nodeID);
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
      long adjustedTotal = (long) (this.batchCount * this.avg) + numTxns;
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

    private void log_stats(float thresh) {
      logger.info(this + " threshold = " + thresh);
    }

    public boolean batchComplete(TransactionID txnID) {
      if (this.txnCount <= 0) {
        // this is possible when the passive server moves to active.
        logger.info("Not decrementing txnCount : " + txnID + " : " + this.toString());
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
      float threshold = (this.avg * (this.batchCount - 1));

      if (false) {
        log_stats(threshold);
      }

      if (this.txnCount <= threshold) {
        if (this.batchCount <= 0) {
          // this is possible when the passive server moves to active.
          logger.info("Not decrementing batchCount : " + txnID + " : " + this.toString());
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
}
