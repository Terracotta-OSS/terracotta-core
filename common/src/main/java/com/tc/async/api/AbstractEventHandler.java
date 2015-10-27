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

import com.tc.logging.TCLogger;
import java.util.Collection;

/**
 * Simple superclass for event handlers that does the iterating over events in the array
 * 
 * @author steve
 */
public abstract class AbstractEventHandler<EC> implements EventHandler<EC> {

  private volatile TCLogger logger;

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

  public TCLogger getLogger() {
    return logger;
  }

  @Override
  public void destroy() {
    //
  }

}
