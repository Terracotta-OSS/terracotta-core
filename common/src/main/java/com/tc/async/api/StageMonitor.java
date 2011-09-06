/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.api;

public interface StageMonitor {
  
  public void eventBegin(int queueDepth);

  public void flush();

  public Analysis analyze();

  public interface Analysis {
    
    public Number getEventCount();

    public Number getElapsedTime();
    
    public Number getEventsPerSecond();

    public Number getMinQueueDepth();

    public Number getMaxQueueDepth();

    public Number getAvgQueueDepth();
    
  }
}
