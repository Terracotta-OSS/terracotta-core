/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.sequence;

public class SequenceBatch {
  private long current;
  private long end;

  public SequenceBatch(long current, long end) {
    this.current = current;
    this.end = end;
  }

  public boolean hasNext() {
    return current < end;
  }

  public long next() {
    return current++;
  }
  
  public String toString() {
    return "SequenceBatch@" + System.identityHashCode(this) +"[ current = " +current + " , end = "+ end + " ]"; 
  }
}