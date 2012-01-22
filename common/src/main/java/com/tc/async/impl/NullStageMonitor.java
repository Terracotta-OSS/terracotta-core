/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
