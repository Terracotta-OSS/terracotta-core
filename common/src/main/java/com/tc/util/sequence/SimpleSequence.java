/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

public class SimpleSequence implements Sequence {
  private long sequence;
  
  public synchronized long next() {
    return ++sequence;
  }

  public synchronized long current() {
    return sequence;
  }

}
