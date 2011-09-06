/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.locks;


public class NullClientLockManagerConfig implements ClientLockManagerConfig {

  private long timeoutInterval = ClientLockManagerConfig.DEFAULT_TIMEOUT_INTERVAL;
  
  public NullClientLockManagerConfig() {
    this.timeoutInterval = ClientLockManagerConfig.DEFAULT_TIMEOUT_INTERVAL; 
  }
  
  public NullClientLockManagerConfig(long timeoutInterval) {
    this.timeoutInterval = timeoutInterval;
  }
  
  public long getTimeoutInterval() {
    return timeoutInterval;
  }
  
  public void setTimeoutInterval(long timeoutInterval) {
    this.timeoutInterval = timeoutInterval;
  }

  public int getStripedCount() {
    return ClientLockManagerConfig.DEFAULT_STRIPED_COUNT;
  }

}
