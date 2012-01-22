/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public interface ClientLockManagerConfig {

  public static final long DEFAULT_TIMEOUT_INTERVAL = 6000;
  public static final int  DEFAULT_STRIPED_COUNT    = 1;

  public long getTimeoutInterval();
  
  public int getStripedCount();

}
