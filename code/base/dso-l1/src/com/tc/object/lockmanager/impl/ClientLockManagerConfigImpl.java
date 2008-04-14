
/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.lockmanager.api.ClientLockManagerConfig;
import com.tc.properties.TCProperties;

public class ClientLockManagerConfigImpl implements ClientLockManagerConfig {
  
  
  private long timeoutInterval;
   
  public ClientLockManagerConfigImpl(TCProperties lockManagerProperties ) {
    this.timeoutInterval = lockManagerProperties.getLong("timeout.interval");  
  }

  public long getTimeoutInterval() {
    return timeoutInterval;
  }
  
  

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.lockmanager.api.ClientLockManagerConfig;
import com.tc.properties.TCProperties;

public class ClientLockManagerConfigImpl implements ClientLockManagerConfig {
  
  
  private long timeoutInterval;
   
  public ClientLockManagerConfigImpl(TCProperties lockManagerProperties ) {
    this.timeoutInterval = lockManagerProperties.getLong("timeout.interval");  
  }

  public long getTimeoutInterval() {
    return timeoutInterval;
  }
  
  

}
