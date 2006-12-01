/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.timedtask;

public class Timed8x2ObjectFault extends TimedObjectFaultBase {

  private static final int READERS = 8;
  private static final int WRITERS = 2;
  
  protected int nodeCount() {
    return READERS + WRITERS;
  }
  
  protected int writerCount() {
    return WRITERS;
  }
}
