/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.locks;

import com.tc.properties.TCProperties;

public class ClientLockManagerConfigImpl implements ClientLockManagerConfig {
  
  private final long timeoutInterval;
  private final int stripedCount;
   
  public ClientLockManagerConfigImpl(TCProperties lockManagerProperties ) {
    this.timeoutInterval = lockManagerProperties.getLong("timeout.interval");  
    this.stripedCount = lockManagerProperties.getInt("striped.count");
  }

  public long getTimeoutInterval() {
    return timeoutInterval;
  }

  public int getStripedCount() {
    return stripedCount;
  }
}
