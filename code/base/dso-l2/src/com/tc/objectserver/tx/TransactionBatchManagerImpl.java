/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class TransactionBatchManagerImpl implements TransactionBatchManager {

  private static final TCLogger logger = TCLogging.getLogger(TransactionBatchManagerImpl.class);

  private final Map             map    = new HashMap();

  public synchronized void defineBatch(ChannelID channelID, TxnBatchID batchID, int numTxns) {
    BatchStats batchStats = getOrCreateStats(channelID);
    batchStats.defineBatch(batchID, numTxns);
  }

  private BatchStats getOrCreateStats(ChannelID channelID) {
    BatchStats bs = (BatchStats) map.get(channelID);
    if (bs == null) {
      bs = new BatchStats(channelID);
      map.put(channelID, bs);
    }
    return bs;
  }

  public synchronized boolean batchComponentComplete(ChannelID channelID, TxnBatchID batchID, TransactionID txnID) {
    BatchStats bs = (BatchStats) map.get(channelID);
    Assert.assertNotNull(bs);
    return bs.batchComplete(batchID, txnID);
  }

  public synchronized void shutdownClient(ChannelID channelID) {
    BatchStats bs = (BatchStats) map.get(channelID);
    if (bs != null) {
      bs.shutdownClient();
    }
  }

  private void cleanUp(ChannelID channelID) {
    map.remove(channelID);
  }

  public class BatchStats {
    private final ChannelID channelID;

    private int             batchCount;
    private int             txnCount;
    private float           avg;

    private boolean         killed = false;

    public BatchStats(ChannelID channelID) {
      this.channelID = channelID;
    }

    public void defineBatch(TxnBatchID batchID, int numTxns) {
      long adjustedTotal = (long) (batchCount * avg) + numTxns;
      txnCount += numTxns;
      batchCount++;
      avg = adjustedTotal / batchCount;
      if (false) log_stats();
    }

    private void log_stats() {
      logger.info(channelID + " : Batch Stats : batch count = " + batchCount + " txnCount = " + txnCount + " avg = "
                  + avg);
    }

    private void log_stats(float thresh) {
      logger.info(channelID + " : Batch Stats : batch count = " + batchCount + " txnCount = " + txnCount + " avg = "
                  + avg + " threshold = " + thresh);
    }

    public boolean batchComplete(TxnBatchID batchID, TransactionID txnID) {
      txnCount--;
      if (killed) {
        // return true only when all txns are acked. Note new batches may still be in network read queue
        if (txnCount == 0) {
          cleanUp(channelID);
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

    public void shutdownClient() {
      this.killed = true;
    }
  }
}
