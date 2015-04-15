/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.tx;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.exception.InvalidSequenceIDException;
import com.tc.l2.ha.TransactionBatchListener;
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
import com.tc.util.SequenceID;
import com.tc.util.SequenceValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TransactionBatchManagerImpl implements TransactionBatchManager, PostInit, PrettyPrintable {

  private static final TCLogger                logger       = TCLogging.getLogger(TransactionBatchManagerImpl.class);

  private final Map<NodeID, BatchStats>        map          = new HashMap<NodeID, BatchStats>();
  private final SequenceValidator              sequenceValidator;
  private final MessageRecycler                messageRecycler;
  private final Object                         lock         = new Object();

  private ServerTransactionManager             transactionManager;
  private TransactionBatchReaderFactory        batchReaderFactory;
  private final TransactionFilter              filter;
  private ServerGlobalTransactionManager       gtxm;
  private DSOChannelManager                    dsoChannelManager;
  private final List<TransactionBatchListener> txnListeners = new CopyOnWriteArrayList<TransactionBatchListener>();
  private final Sink                           syncWriteTxnRecvdSink;
  private final ResentTransactionSequencer     resentTransactionSequencer;

  public TransactionBatchManagerImpl(final SequenceValidator sequenceValidator, final MessageRecycler recycler,
                                     final TransactionFilter txnFilter, final Sink syncWriteTxnRecvdSink,
                                     final ResentTransactionSequencer resentTransactionSequencer) {
    this.sequenceValidator = sequenceValidator;
    this.messageRecycler = recycler;
    this.filter = txnFilter;
    this.syncWriteTxnRecvdSink = syncWriteTxnRecvdSink;
    this.resentTransactionSequencer = resentTransactionSequencer;
  }

  @Override
  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    this.transactionManager = oscc.getTransactionManager();
    this.gtxm = oscc.getServerGlobalTransactionManager();
    this.dsoChannelManager = oscc.getChannelManager();
  }

  @Override
  public void addTransactionBatch(final CommitTransactionMessage ctm) {
    fireBatchTxnEvent(ctm);
    try {
      final TransactionBatchReader reader = this.batchReaderFactory.newTransactionBatchReader(ctm);

      ServerTransaction txn;

      // Transactions should maintain order.
      final List<ServerTransaction> txns = new ArrayList<ServerTransaction>(reader.getNumberForTxns());
      final Set<ServerTransactionID> txnIDs = new HashSet<ServerTransactionID>(reader.getNumberForTxns());
      final NodeID nodeID = reader.getNodeID();
      final Set<ObjectID> newObjectIDs = new HashSet<ObjectID>();
      final Set<TransactionID> syncWriteTxns = new HashSet<TransactionID>();
      
      SequenceID current = SequenceID.NULL_ID;

      while ((txn = reader.getNextTransaction()) != null) {
        if ( current.toLong() > txn.getClientSequenceID().toLong() ) {
          throw new InvalidSequenceIDException("sequence moves backward");
        }
        current = txn.getClientSequenceID();
        txns.add(txn);
        txnIDs.add(txn.getServerTransactionID());
        newObjectIDs.addAll(txn.getNewObjectIDs());
        if (txn.getTransactionType().equals(TxnType.SYNC_WRITE)) {
          syncWriteTxns.add(txn.getTransactionID());
        }
      }
      
      this.sequenceValidator.advanceCurrent(nodeID, current);

      defineBatch(nodeID, txns.size());

      this.filter.addTransactionBatch(new IncomingTransactionBatchContext(nodeID, txnIDs, reader, txns, newObjectIDs));

      if (reader.containsSyncWriteTransaction()) {
        this.syncWriteTxnRecvdSink.add(new SyncWriteTransactionReceivedContext(reader.getBatchID().toLong(),
                                                                               (ClientID) ctm.getSourceNodeID(),
                                                                               syncWriteTxns));
      }

      this.messageRecycler.addMessage(ctm, txnIDs);
    } catch (final Exception e) {
      logger.error("Error reading transaction batch. : ", e);
      final MessageChannel c = ctm.getChannel();
      logger.error("Closing channel " + c.getChannelID() + " due to previous errors !");
      c.close();
    }
  }

  @Override
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
        resentTransactionSequencer.addTransactions(batchContext);
      } catch (final Exception e) {
        logger.error("Error reading transaction batch. : ", e);
        logger.error("Closing channel " + nodeID + " due to previous errors !");
        this.dsoChannelManager.closeAll(Collections.singletonList(nodeID));
      }
    }
  }

  @Override
  public void defineBatch(final NodeID nid, final int numTxns) {
    final BatchStats batchStats = getOrCreateStats(nid);
    batchStats.defineBatch(numTxns);
  }

  private synchronized BatchStats getOrCreateStats(final NodeID nid) {
    BatchStats bs = this.map.get(nid);
    if (bs == null) {
      bs = new BatchStats(nid);
      this.map.put(nid, bs);
    }
    return bs;
  }
  
  private synchronized BatchStats getStats(final NodeID nid) {
    return this.map.get(nid);
  }

  @Override
  public boolean batchComponentComplete(final NodeID nid, final TransactionID txnID) {
    final BatchStats bs = getStats(nid);
    Assert.assertNotNull(bs);
    BatchStats.BatchState complete = bs.batchComplete(txnID);
    try {
      return complete.isComplete();
    } finally {
      if ( complete.isShutdown() ) {
        cleanUp(nid);
      }
    }
  }

  @Override
  public void nodeConnected(final NodeID nodeID) {
    this.transactionManager.nodeConnected(nodeID);
  }

  @Override
  public void notifyServerHighWaterMark(final NodeID nodeID, final long serverHighWaterMark) {
    this.filter.notifyServerHighWaterMark(nodeID, serverHighWaterMark);
  }

  @Override
  public void shutdownNode(final NodeID nodeID) {
    if (this.filter.shutdownNode(nodeID)) {
      shutdownBatchStats(nodeID);
      this.transactionManager.shutdownNode(nodeID);
    } else {
      logger.warn("Not clearing shutdownNode : " + nodeID);

    }
  }

  private void shutdownBatchStats(final NodeID nodeID) {
    final BatchStats bs = getStats(nodeID);
    if (bs != null) {
      if ( bs.shutdownNode() ) {
//  no more transactions inflight, remove it
        cleanUp(nodeID);
      }
    }
  }

  private synchronized void cleanUp(final NodeID nodeID) {
    this.map.remove(nodeID);
  }

  public static class BatchStats {
    private final NodeID nodeID;

    private int          batchCount;
    private int          txnCount;
    private int          ackCount;

    private boolean      killed = false;
    public static enum BatchState {
      COMPLETE {
        @Override
        public boolean isComplete() { return true; }
      },
      SHUTDOWN {
        @Override
        public boolean isComplete() { return true; }
        @Override
        public boolean isShutdown() { return true; }
      },
      CONTINUE;
      public boolean isComplete() { return false; }
      public boolean isShutdown() {  return false; }
    }

    public BatchStats(final NodeID nid) {
      this.nodeID = nid;
    }

    public synchronized void defineBatch(final int numTxns) {
      this.txnCount += numTxns;
      if ( this.batchCount++ == 0 ) {
        resetAckCount();
      }
    }
    
    private void resetAckCount() {
      this.ackCount = Integer.SIZE - Integer.numberOfLeadingZeros(txnCount) > batchCount-1 ? 
          txnCount >> (batchCount-1) : 1;
      if ( this.ackCount < 0 || (this.ackCount == 0 && ( (this.batchCount | this.txnCount) != 0 ) ) ) {
        throw new AssertionError(this.toString());
      }
      logger.debug(this);
    }

    @Override
    public String toString() {
      return "BatchStats : " + this.nodeID + " : batch count = " + this.batchCount + " txnCount = " + this.txnCount + " ackCount=" + this.ackCount;
    }

    public synchronized BatchState batchComplete(final TransactionID txnID) {
      if (this.txnCount <= 0) {
        // this is possible when the passive server moves to active.
        logger.info("Not decrementing txnCount : " + txnID + " : " + toString());
      } else {
        this.txnCount--;
      }
      if (this.killed) {
        // return true only when all TXNs are ACKed. Note new batches may still be in network read queue
        return (this.txnCount == 0) ? BatchState.SHUTDOWN : BatchState.CONTINUE;
      }

      if ( --this.ackCount <= 0 ) {
        if (this.batchCount <= 0) {
          // this is possible when the passive server moves to active.
          logger.info("Not decrementing batchCount : " + txnID + " : " + toString());
        } else {
          this.batchCount--;
        }
        resetAckCount();
        return BatchState.COMPLETE;
      } else {
        return BatchState.CONTINUE;
      }
    }

    public synchronized boolean shutdownNode() {
      this.killed = true;
      return (this.txnCount == 0);
    }
  }

  @Override
  public void registerForBatchTransaction(final TransactionBatchListener listener) {
    this.txnListeners.add(listener);
  }

  private void fireBatchTxnEvent(final CommitTransactionMessage ctm) {
    for (final TransactionBatchListener listener : this.txnListeners) {
      listener.notifyTransactionBatchAdded(ctm);
    }
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.print("BatchStats: " + this.map.size()).flush();
    for (final Entry<NodeID, BatchStats> e : this.map.entrySet()) {
      out.duplicateAndIndent().indent().print(e.getKey() + " => " + e.getValue()).flush();
    }
    return out;
  }
}
