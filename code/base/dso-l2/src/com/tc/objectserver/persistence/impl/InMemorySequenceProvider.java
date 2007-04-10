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
  private long         id  = 0;

  public synchronized String getUID() {
    return uid;
  }

  public synchronized long next() {
    return id++;
  }

  public synchronized long nextBatch(int batchSize) {
    long lid = id;
    id += batchSize;
    return lid;
  }

  public synchronized void setNext(long next) {
    Assert.assertTrue(id <= next);
    id = next;
  }

}
