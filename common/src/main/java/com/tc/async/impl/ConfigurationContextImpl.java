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
package com.tc.async.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;

/**
 * Used to initialize and event handlers. This needs to grow up a lot. I want to beable to have null stages and tracing
 * stages and all kinds of crazy useful stuff. But I don't want to bog down so I'm going to add stuff as I need it for
 * now.
 * 
 * @author steve
 */
public class ConfigurationContextImpl implements ConfigurationContext {

  private final StageManager stageManager;

  public ConfigurationContextImpl(StageManager stageManager) {
    this.stageManager = stageManager;
  }

  @Override
  public <EC> Stage<EC> getStage(String name, Class<EC> verification) {
    return stageManager.getStage(name, verification);
  }

  @Override
  public Logger getLogger(Class<?> clazz) {
    return LoggerFactory.getLogger(clazz);
  }
}
