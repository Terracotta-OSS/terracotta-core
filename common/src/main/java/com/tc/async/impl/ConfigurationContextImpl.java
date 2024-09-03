/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
  private final String identifier;

  public ConfigurationContextImpl(String identifier, StageManager stageManager) {
    this.identifier = identifier;
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

  @Override
  public String getIdentifier() {
    return identifier;
  }
}
