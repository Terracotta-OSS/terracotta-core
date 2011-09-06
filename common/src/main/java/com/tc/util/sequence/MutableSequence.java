/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.sequence;

public interface MutableSequence extends Sequence {

  // This provide a unique id for an instance of the sequence.
  public String getUID();

  public long nextBatch(long batchSize);
  
  public void setNext(long next);

}