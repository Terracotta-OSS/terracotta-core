/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
