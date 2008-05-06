/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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