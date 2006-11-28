/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.timedtask;

public class Timed2xChangeReplication extends TimedChangeReplicationBase {

  protected int nodeCount() {
    return 3; // + 1 for writer node
  }
}
