/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.properties;

public interface ReconnectConfig {

  default boolean getReconnectEnabled() {
    return true;
  }

  default int getReconnectTimeout() {
    return 5000;
  }
  
  default int getSendQueueCapacity() {
    return 5000;
  }
  
  default int getMaxDelayAcks() {
    return 16;
  }
  
  default int getSendWindow() {
    return 32;
  }
  
}
