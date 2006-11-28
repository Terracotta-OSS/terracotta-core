/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.faulting;


public class SingleQueueFault4Node extends SingleQueueFaultBase {

  protected int nodeCount() {
    return 5; // + 1 for writer node
  }
}
