/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
