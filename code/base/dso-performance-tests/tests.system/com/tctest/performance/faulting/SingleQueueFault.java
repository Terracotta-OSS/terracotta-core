/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.faulting;

public class SingleQueueFault extends SingleQueueFaultBase {

  protected int nodeCount() {
    return 2; // + 1 for writer node
  }
}
