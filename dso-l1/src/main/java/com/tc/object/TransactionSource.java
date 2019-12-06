/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
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
