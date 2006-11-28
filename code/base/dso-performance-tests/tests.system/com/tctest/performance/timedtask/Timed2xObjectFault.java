/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.timedtask;

public class Timed2xObjectFault extends TimedObjectFaultBase {

  private static final int READERS = 2;
  private static final int WRITERS = 1;
  
  protected int nodeCount() {
    return READERS + WRITERS;
  }
  
  protected int writerCount() {
    return WRITERS;
  }
}
