/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.StageMonitor;

public class NullStageMonitor implements StageMonitor {

  public NullStageMonitor() {
    return;
  }

  @Override
  public void eventBegin(int queueDepth) {
    return;
  }

  @Override
  public void flush() {
    return;
  }

  @Override
  public Analysis analyze() {
    return null;
  }
}
