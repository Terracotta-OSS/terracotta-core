/*
 * Created on May 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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
