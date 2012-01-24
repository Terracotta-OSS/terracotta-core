/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.properties;

public interface ReconnectConfig {

  public boolean getReconnectEnabled();

  public int getReconnectTimeout();
  
  public int getSendQueueCapacity();
  
  public int getMaxDelayAcks();
  
  public int getSendWindow();
  
}
