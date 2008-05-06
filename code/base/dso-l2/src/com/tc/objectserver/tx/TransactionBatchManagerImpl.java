/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.NodeID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class TransactionBatchManagerImpl implements TransactionBatchManager {

  private static final TCLogger logger = TCLogging.getLogger(TransactionBatchManagerImpl.class);

  private final Map             map    = new HashMap();

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

  public synchronized void shutdownNode(NodeID nodeID) {
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
      logger
          .info(nodeID + " : Batch Stats : batch count = " + batchCount + " txnCount = " + txnCount + " avg = " + avg);
    }

    private void log_stats(float thresh) {
      logger.info(nodeID + " : Batch Stats : batch count = " + batchCount + " txnCount = " + txnCount + " avg = " + avg
                  + " threshold = " + thresh);
    }

    public boolean batchComplete(TransactionID txnID) {
      txnCount--;
      if (killed) {
        // return true only when all txns are acked. Note new batches may still be in network read queue
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
        batchCount--;
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
