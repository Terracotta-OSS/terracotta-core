/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Collection;
import java.util.Iterator;

/**
 * Simple superclass for event handlers that does the iterating over events in the array
 * 
 * @author steve
 */
public abstract class AbstractEventHandler implements EventHandler {

  private ConfigurationContext configContext;
  private TCLogger             logger;

  public abstract void handleEvent(EventContext context) throws EventHandlerException;

  public void handleEvents(Collection contexts) throws EventHandlerException {
    for (Iterator i = contexts.iterator(); i.hasNext();) {
      EventContext eh = (EventContext) i.next();
      handleEvent(eh);
    }
  }

  public synchronized final void initializeContext(ConfigurationContext context) {
    if (context == null) {
      this.logger = TCLogging.getLogger(this.getClass());
      logger.warn("Setting config context to null. This is highly unusual");
    } else {
      this.logger = context.getLogger(this.getClass());
    }
    this.configContext = context;
    initialize(context);
  }

  protected void initialize(ConfigurationContext context) {
    // Subclasses can override this.
  }

  public TCLogger getLogger() {
    return logger;
  }

  public synchronized void destroy() {
    configContext = null;
  }

  /**
   * @return the ConfigurationContext object that was passed to the <code>initialize</code> method.
   */
  protected synchronized ConfigurationContext getConfigurationContext() {
    return configContext;
  }

}
