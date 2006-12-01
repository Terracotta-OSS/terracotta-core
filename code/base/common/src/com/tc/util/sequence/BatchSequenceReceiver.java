/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.sequence;

public interface BatchSequenceReceiver {
  public void setNextBatch(long start, long end);
  
  public boolean hasNext();
}
