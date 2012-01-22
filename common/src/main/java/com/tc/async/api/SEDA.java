/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.util.concurrent.QueueFactory;

/**
 * Manages the startup and shutdown of a SEDA environment
 * 
 * @author steve
 */
public class SEDA {
  private final StageManager  stageManager;
  private final TCThreadGroup threadGroup;

  public SEDA(final TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.stageManager = new StageManagerImpl(threadGroup, new QueueFactory());
  }

  public StageManager getStageManager() {
    return stageManager;
  }

  protected TCThreadGroup getThreadGroup() {
    return this.threadGroup;
  }
}
