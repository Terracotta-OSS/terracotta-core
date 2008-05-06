/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

public class SequenceGenerator {

  private long seq;

  public SequenceGenerator() {
    this(0);
  }

  public SequenceGenerator(long start) {
    this.seq = start - 1;
  }
  
  public synchronized long getNextSequence() {
    return ++seq;
  }

  public synchronized long getCurrentSequence() {
    return seq;
  }
}
