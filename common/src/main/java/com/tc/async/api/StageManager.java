/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.async.api;

import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.text.PrettyPrintable;

import java.util.List;


public interface StageManager extends PrettyPrintable {
  public <EC> Stage<EC> createStage(String name, Class<EC> verification, EventHandler<EC> handler, int threads);
  public <EC> Stage<EC> createStage(String name, Class<EC> verification, EventHandler<EC> handler, int threads, int maxSize, boolean canBeDirect, boolean stallWarn);
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
  
  public int getDefaultStageMaximumCapacity();
}
