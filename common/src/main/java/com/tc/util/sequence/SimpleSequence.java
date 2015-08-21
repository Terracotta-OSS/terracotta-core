/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

public class SimpleSequence implements Sequence {
  private long sequence;
  
  @Override
  public synchronized long next() {
    return ++sequence;
  }

  @Override
  public synchronized long current() {
    return sequence;
  }

}
