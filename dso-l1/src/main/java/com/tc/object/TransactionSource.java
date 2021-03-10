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
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionSource {
// older servers don't like zero transasction ids so start at 1L
  private final AtomicLong current = new AtomicLong(1L);
  private volatile TransactionID oldestCache = new TransactionID(1L);
  private long retiredBase = 1L;
  private int retirePosition = 0;
  private final long GC_THRESHOLD = 32 * 1024;
  private BitSet retired = new BitSet();

  public TransactionID create() {
    return new TransactionID(current.incrementAndGet());
  }

  public TransactionID oldest() {
    return oldestCache;
  }

  private void updateOldest() {
    retirePosition = retired.nextClearBit(retirePosition);
    long transactionId = retirePosition + retiredBase;
    if (oldestCache.toLong() != transactionId) {
      oldestCache = new TransactionID(transactionId);
    }
  }

  private void gc() {
    if (retirePosition > GC_THRESHOLD) {
      retiredBase += retirePosition;
      retired = retired.get(retirePosition, retired.size());
      retirePosition = 0;
    }
  }

  public synchronized boolean retire(TransactionID txnId) {
    int index = (int)(txnId.toLong() - retiredBase);
    boolean last = !retired.get(index);
    retired.set(index);
    updateOldest();
    gc();
    return last;
  }
}
