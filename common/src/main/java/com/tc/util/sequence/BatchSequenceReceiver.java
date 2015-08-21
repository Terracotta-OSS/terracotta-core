/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

public interface BatchSequenceReceiver {
  public void setNextBatch(long start, long end);
  
  public boolean isBatchRequestPending();
}
