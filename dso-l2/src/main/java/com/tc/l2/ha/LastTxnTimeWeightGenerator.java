/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.util.Assert;

import java.util.concurrent.atomic.AtomicLong;

public class LastTxnTimeWeightGenerator implements WeightGenerator, TransactionBatchListener {
  private final AtomicLong lastTxnTime = new AtomicLong(Long.MIN_VALUE);

  public LastTxnTimeWeightGenerator(TransactionBatchManager transactionBatchManager) {
    Assert.assertNotNull(transactionBatchManager);
    transactionBatchManager.registerForBatchTransaction(this);
  }

  /*
   * return (weight-generation-time - last-batch-transaction-time) return 0 if none txn yet.
   * negative weight, closest one win.
   */
  public long getWeight() {
    long last = lastTxnTime.get();
    return (last == Long.MIN_VALUE) ? last : last - System.nanoTime();
  }

  public void notifyTransactionBatchAdded(CommitTransactionMessage ctm) {
    lastTxnTime.set(System.nanoTime());
  }
}
