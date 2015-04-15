/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.async.api;

import com.tc.logging.TCLoggerProvider;
import com.tc.object.ClearableCallback;
import com.tc.stats.Stats;
import com.tc.text.PrettyPrintable;

import java.util.Collection;
import java.util.List;

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
public interface StageManager extends PrettyPrintable, ClearableCallback {
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
