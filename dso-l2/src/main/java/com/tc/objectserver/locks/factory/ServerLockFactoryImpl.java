/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.factory;

import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.ServerLock;
import com.tc.objectserver.locks.LockFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ServerLockFactoryImpl implements LockFactory {
  private final static boolean GREEDY_LOCKS_ENABLED = TCPropertiesImpl
                                                     .getProperties()
                                                     .getBoolean(TCPropertiesConsts.L2_LOCKMANAGER_GREEDY_LOCKS_ENABLED);
  private final LockFactory    factory;

  public ServerLockFactoryImpl() {
    if (GREEDY_LOCKS_ENABLED) {
      factory = new GreedyPolicyFactory();
    } else {
      factory = new NonGreedyLockPolicyFactory();
    }
  }

  public ServerLock createLock(LockID lid) {
    return factory.createLock(lid);
  }
}
