/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
