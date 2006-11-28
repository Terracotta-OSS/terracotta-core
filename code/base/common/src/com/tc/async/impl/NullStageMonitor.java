/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.StageMonitor;

public class NullStageMonitor implements StageMonitor {

  public NullStageMonitor() {
    return;
  }

  public void eventBegin(int queueDepth) {
    return;
  }

  public void flush() {
    return;
  }

  public Analysis analyze() {
    return null;
  }
}