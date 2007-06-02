/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.util.Assert;
import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

public class InMemorySequenceProvider implements MutableSequence {

  private final String uid = UUID.getUUID().toString();
  private long         nextID  = 0;

  public synchronized String getUID() {
    return uid;
  }

  public synchronized long next() {
    return nextID++;
  }

  public synchronized long current() {
    return nextID - 1;
  }

  public synchronized long nextBatch(int batchSize) {
    long lid = nextID;
    nextID += batchSize;
    return lid;
  }

  public synchronized void setNext(long next) {
    Assert.assertTrue(nextID <= next);
    nextID = next;
  }

}
