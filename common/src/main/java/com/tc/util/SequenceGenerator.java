/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
