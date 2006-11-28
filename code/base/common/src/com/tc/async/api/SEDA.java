/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.async.api;

import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;

/**
 * Manages the startup and shutdown of a SEDA environment
 *
 * @author steve
 */
public class SEDA {
  private final StageManager stageManager;
  private final TCThreadGroup threadGroup;

  public SEDA(TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.stageManager = new StageManagerImpl(threadGroup);
  }

  public StageManager getStageManager() {
    return stageManager;
  }

  protected TCThreadGroup getThreadGroup() {
    return this.threadGroup;
  }
}