/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
