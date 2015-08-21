/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

public interface MutableSequence extends Sequence {

  // This provide a unique id for an instance of the sequence.
  public String getUID();

  public long nextBatch(long batchSize);
  
  public void setNext(long next);

}
