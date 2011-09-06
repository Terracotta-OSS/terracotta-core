/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.factory;

import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.ServerLock;
import com.tc.objectserver.locks.LockFactory;
import com.tc.objectserver.locks.NonGreedyServerLock;

public class NonGreedyLockPolicyFactory implements LockFactory {
  public ServerLock createLock(LockID lid) {
    return new NonGreedyServerLock(lid);
  }
}