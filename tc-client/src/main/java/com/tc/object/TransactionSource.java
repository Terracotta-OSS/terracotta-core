/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
    return new TransactionID(current.getAndIncrement());
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
