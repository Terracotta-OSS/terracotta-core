/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.faulting;

public class DualQueueFault8Node extends DualQueueFaultBase {

  protected int nodeCount() {
    return 9; // + 1 for writer node
  }
}
