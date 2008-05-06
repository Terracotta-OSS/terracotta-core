/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

/**
 * Used to initialize and event handlers. This needs to grow up a lot. I want to beable to have null stages and tracing
 * stages and all kinds of crazy useful stuff. But I don't want to bog down so I'm going to add stuff as I need it for
 * now.
 * 
 * @author steve
 */
public class ConfigurationContextImpl implements ConfigurationContext {

  private StageManager stageManager;

  public ConfigurationContextImpl(StageManager stageManager) {
    this.stageManager = stageManager;
  }

  public Stage getStage(String name) {
    return stageManager.getStage(name);
  }

  public TCLogger getLogger(Class clazz) {
    return TCLogging.getLogger(clazz);
  }
}