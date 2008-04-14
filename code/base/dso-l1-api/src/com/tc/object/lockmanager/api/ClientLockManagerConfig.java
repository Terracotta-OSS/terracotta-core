/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

public interface ClientLockManagerConfig {
  
  public static final long DEFAULT_TIMEOUT_INTERVAL = 6000;
  
  
  public long getTimeoutInterval();

}
