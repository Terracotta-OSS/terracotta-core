/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
  private final StageManager stageManager;
  private final TCThreadGroup threadGroup;

  public SEDA(TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.stageManager = new StageManagerImpl(threadGroup, new QueueFactory());
  }

  public SEDA(TCThreadGroup threadGroup, String className){
    this.threadGroup = threadGroup;
    this.stageManager = new StageManagerImpl(threadGroup, new QueueFactory(className));
  }
  
  public StageManager getStageManager() {
    return stageManager;
  }

  protected TCThreadGroup getThreadGroup() {
    return this.threadGroup;
  }
}