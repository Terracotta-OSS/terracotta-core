package com.tc.async.api;

import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;

import java.util.Collection;

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
/**
 * @author steve This is the interface for an early version of our custom seda implementation take it with a grain of
 *         salt
 */
public interface StageManager {
  public Stage createStage(String name, EventHandler handler, int threads, int maxSize);

  public void startStage(Stage stage, ConfigurationContext context);

  public void startAll(ConfigurationContext context);

  public void stopStage(Stage stage);

  public void stopAll();

  public Stage getStage(String name);

  public void setLoggerProvider(TCLoggerProvider loggerProvider);

  public Stats[] getStats();

  public Collection getStages();
}