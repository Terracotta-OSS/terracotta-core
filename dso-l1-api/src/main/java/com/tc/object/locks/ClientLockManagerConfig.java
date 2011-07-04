/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

public interface ClientLockManagerConfig {

  public static final long DEFAULT_TIMEOUT_INTERVAL = 6000;
  public static final int  DEFAULT_STRIPED_COUNT    = 1;

  public long getTimeoutInterval();
  
  public int getStripedCount();

}
