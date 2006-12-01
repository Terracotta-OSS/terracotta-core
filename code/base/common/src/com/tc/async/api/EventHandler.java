/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.api;

import java.util.Collection;

/**
 * @author steve Interface for handling either single events or multiple events at one time. For more information of
 *         this kind of stuff google SEDA -Staged Event Driven Architecure
 */
public interface EventHandler {

  /**
   * Handle one event at a time. Called by the StageController
   * 
   * @param context
   * @throws EventHandlerException
   */
  public void handleEvent(EventContext context) throws EventHandlerException;

  /**
   * Handle multiple events at once in a batch. This can be more performant because it avoids context switching
   * 
   * @param context
   * @throws EventHandlerException
   */
  public void handleEvents(Collection context) throws EventHandlerException;

  /**
   * Initialize the state of the EventHandler
   * 
   * @param context
   */
  public void initialize(ConfigurationContext context);

  /**
   * Shut down the stage
   */
  public void destroy();

  public void logOnEnter(EventContext context);
  
  public void logOnExit(EventContext context);
}