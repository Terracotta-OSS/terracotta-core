/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.timedtask;

public class Timed4xChangeReplication extends TimedChangeReplicationBase {

  protected int nodeCount() {
    return 5; // + 1 for writer node
  }
}
