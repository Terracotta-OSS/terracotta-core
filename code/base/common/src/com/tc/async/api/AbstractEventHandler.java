/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.api;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

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
  private final ThreadLocal    start       = new ThreadLocal();
  private boolean              initialized = false;

  public abstract void handleEvent(EventContext context) throws EventHandlerException;

  public void handleEvents(Collection contexts) throws EventHandlerException {
    for (Iterator i = contexts.iterator(); i.hasNext();) {
      EventContext eh = (EventContext) i.next();
      handleEvent(eh);
    }
  }

  public synchronized void initialize(ConfigurationContext context) {
    if (context == null) {
      this.logger = TCLogging.getLogger(this.getClass());
      logger.warn("Setting config context to null. This is highly unusual");
    } else {
      this.logger = context.getLogger(this.getClass());
    }
    this.configContext = context;
    this.initialized = true;
  }

  public TCLogger getLogger() {
    return logger;
  }

  public synchronized void destroy() {
    configContext = null;
    this.initialized = false;
  }

  /**
   * @return the ConfigurationContext object that was passed to the <code>initialize</code> method.
   */
  protected synchronized ConfigurationContext getConfigurationContext() {
    return configContext;
  }

  public void logOnEnter(EventContext eventContext) {
    logOnEnter(eventContext, getLogger());
  }

  public void logOnEnter(EventContext eventContext, TCLogger localLogger) {
    Assert.eval("This usually happens because someone is overriding the initialize method without calling super",
                initialized);
    if (localLogger.isDebugEnabled()) {
      markStart();
      localLogger.debug("handler.handleEvent() start: <event-handler>" + this + "</event-handler><event-context>"
                        + eventContext + "</event-context>");
    }
  }

  public void logOnExit(EventContext eventContext) {
    logOnExit(eventContext, logger);
  }

  public void logOnExit(EventContext eventContext, TCLogger localLogger) {
    Assert.eval(initialized);
    if (localLogger.isDebugEnabled()) {
      localLogger.debug("handler.handleEvent() stop: <elapsed-time>" + getExecutionTime()
                        + "</elapsed-time><event-handler>" + this + "</event-handler><event-context>" + eventContext
                        + "</event-context>");
    }
  }

  private void markStart() {
    start.set(new Long(System.currentTimeMillis()));
  }

  private long getExecutionTime() {
    return System.currentTimeMillis() - ((Long) start.get()).longValue();
  }
}