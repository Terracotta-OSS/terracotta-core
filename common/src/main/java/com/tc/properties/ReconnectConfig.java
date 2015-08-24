/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.properties;

public interface ReconnectConfig {

  boolean getReconnectEnabled();

  int getReconnectTimeout();
  
  int getSendQueueCapacity();
  
  int getMaxDelayAcks();
  
  int getSendWindow();
  
}
