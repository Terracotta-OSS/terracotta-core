/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

/**
 * This type of context has a build in execute() method which is run instead of the handler's handleEvent() method.
 * These contexts are used to run some code inband to do stuff after all the other queued event contexts are executed.
 */
public interface SpecializedEventContext extends MultiThreadedEventContext {

  public void execute() throws EventHandlerException;
}
