/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.faulting;

public class SingleQueueFault extends SingleQueueFaultBase {

  protected int nodeCount() {
    return 2; // + 1 for writer node
  }
}
