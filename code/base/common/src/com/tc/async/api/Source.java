/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.api;

/**
 * This is used by the internals to manage the process of processing EventContexts in the manner that makes sense for
 * each one. Individual Stages SHOULD NOT HAVE TO EITHER USE OR IMPLEMENT THIS INTERFACE
 * 
 */

public interface Source {

  public EventContext poll(long period) throws InterruptedException;

  public String getSourceName();

}