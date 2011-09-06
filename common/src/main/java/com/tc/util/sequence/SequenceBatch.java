/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

public class SequenceBatch {
  private long next;
  private long end;

  public SequenceBatch(long next, long end) {
    this.next = next;
    this.end = end;
  }

  public boolean hasNext() {
    return next < end;
  }

  public long next() {
    return next++;
  }

  public long current() {
    return next - 1;
  }
  
  public long end() {
    return end;
  }

  public String toString() {
    return "SequenceBatch@" + System.identityHashCode(this) + "[ next = " + next + " , end = " + end + " ]";
  }
}