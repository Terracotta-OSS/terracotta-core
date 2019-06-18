package com.tc.object;

import com.tc.object.tx.TransactionID;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionSource {

  private static final AtomicLong nextTransactionId = new AtomicLong(1L);

  private static final ConcurrentSkipListSet<Long> pending = new ConcurrentSkipListSet<>();

  public TransactionID create() {
    long txn;
    while (!pending.add((txn = nextTransactionId.getAndIncrement())));
    return new TransactionID(txn);
  }

  public TransactionID oldest() {
    Long first = pending.first();
    if (first == null) {
      return null;
    } else {
      return new TransactionID(first);
    }
  }

  public boolean retire(TransactionID txnId) {
    return pending.remove(txnId.toLong());
  }
}
