/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.util.Assert;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.MutableSequence;

public final class TestMutableSequence implements MutableSequence {

  public long                         sequence       = 0;
  public final NoExceptionLinkedQueue nextBatchQueue = new NoExceptionLinkedQueue();

  public long next() {
    return ++sequence;
  }
  
  public long current() {
    return sequence;
  }

  public long nextBatch(long batchSize) {
    nextBatchQueue.put(new Object[] { new Integer((int)batchSize) });
    long ls = sequence;
    sequence += batchSize;
    return ls;
  }

  public String getUID() {
    throw new ImplementMe();
  }

  public void setNext(long next) {
    Assert.assertTrue(this.sequence <= next);
    sequence = next;
  }

}
