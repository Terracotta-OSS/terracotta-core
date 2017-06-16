/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.async.api;

import org.terracotta.entity.StateDumpable;

import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.text.PrettyPrintable;

import java.util.List;


public interface StageManager extends PrettyPrintable, StateDumpable {
  public <EC> Stage<EC> createStage(String name, Class<EC> verification, EventHandler<EC> handler, int threads, int maxSize);
/**
 * Start all the stages created on this stage manager.
 * @param context 
 * @param toInit these items are executed before stage start
 * @param exclusion a list of stage names that should not be started.  
 */
  public void startAll(ConfigurationContext context, List<PostInit> toInit, String...exclusion);
  
  public void stopAll();

  public <EC> Stage<EC> getStage(String name, Class<EC> verification);

  public void setLoggerProvider(TCLoggerProvider loggerProvider);

  public Stats[] getStats();

  public void cleanup();
}
