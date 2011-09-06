/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.runner;

import com.tc.simulator.app.GlobalIdGenerator;



public final class TestGlobalIdGenerator implements GlobalIdGenerator {
  int count = 0;

  public synchronized long nextId() {
    return count++;
  }
}