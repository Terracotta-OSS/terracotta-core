/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.timedtask;

public class Timed8xChangeReplication extends TimedChangeReplicationBase {

  protected int nodeCount() {
    return 9; // + 1 for writer node
  }
}
