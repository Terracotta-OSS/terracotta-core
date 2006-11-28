/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.runner;

import com.tc.simulator.app.GlobalIdGenerator;



public final class TestGlobalIdGenerator implements GlobalIdGenerator {
  int count = 0;

  public synchronized long nextId() {
    return count++;
  }
}