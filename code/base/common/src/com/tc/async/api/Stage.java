/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.api;
/**
 * @author steve
 */
public interface Stage {

  public final static int NO_MAX_QUEUE_SIZE = -1;

  public void destroy();

  public Sink getSink();

  public String getName();

  public void start(ConfigurationContext context);
  
  public void pause();
  
  public void unpause();
}