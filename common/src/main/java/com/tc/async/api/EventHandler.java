/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.api;

import java.util.Collection;

/**
 * Interface for handling either single events or multiple events at one time. For more information of this kind of
 * stuff Google SEDA -Staged Event Driven Architecture
 */
public interface EventHandler extends PostInit {

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
   * Shut down the stage
   */
  public void destroy();

}