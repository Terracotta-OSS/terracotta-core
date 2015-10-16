/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.text.PrettyPrintable;

import java.util.List;


/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
public interface StageManager extends PrettyPrintable {
  public <EC> Stage<EC> createStage(String name, Class<EC> verification, EventHandler<EC> handler, int threads, int maxSize);

  public void startAll(ConfigurationContext context, List<PostInit> toInit);

  public void stopAll();

  public <EC> Stage<EC> getStage(String name, Class<EC> verification);

  public void setLoggerProvider(TCLoggerProvider loggerProvider);

  public Stats[] getStats();

  public void cleanup();
}
