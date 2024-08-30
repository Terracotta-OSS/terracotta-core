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
package com.tc.async.api;

import org.slf4j.Logger;

import java.util.Collection;

/**
 * Simple superclass for event handlers that does the iterating over events in the array
 * 
 * @author steve
 */
public abstract class AbstractEventHandler<EC> implements EventHandler<EC> {

  private volatile Logger logger;

  @Override
  public abstract void handleEvent(EC context) throws EventHandlerException;

  @Override
  public void handleEvents(Collection<EC> contexts) throws EventHandlerException {
    for (EC context : contexts) {
      handleEvent(context);
    }
  }

  @Override
  public synchronized final void initializeContext(ConfigurationContext context) {
    this.logger = context.getLogger(this.getClass());
    initialize(context);
  }

  protected void initialize(ConfigurationContext context) {
    // Subclasses can override this.
  }

  public Logger getLogger() {
    return logger;
  }

  @Override
  public void destroy() {
    //
  }

}
