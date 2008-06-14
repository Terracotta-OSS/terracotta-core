/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

public interface ReconnectConfig {

  public boolean getReconnectEnabled();

  public int getReconnectTimeout();
  
  public int getSendQueueCapacity();
  
  public int getMaxDelayAcks();
  
  public int getSendWindow();
  
}
