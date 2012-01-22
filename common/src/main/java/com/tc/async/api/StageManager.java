/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.text.PrettyPrintable;

import java.util.Collection;
import java.util.List;

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
public interface StageManager extends PrettyPrintable {
  public Stage createStage(String name, EventHandler handler, int threads, int maxSize);

  public Stage createStage(String name, EventHandler handler, int threads, int queueRatio, int maxSize);

  public void startStage(Stage stage, ConfigurationContext context);

  public void startAll(ConfigurationContext context, List<PostInit> toInit);

  public void stopStage(Stage stage);

  public void stopAll();

  public Stage getStage(String name);

  public void setLoggerProvider(TCLoggerProvider loggerProvider);

  public Stats[] getStats();

  public Collection<Stage> getStages();
}
