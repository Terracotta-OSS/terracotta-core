/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.object.locks.LockID;

public interface LockMBean {
  public LockID getLockID();

  public ServerLockContextBean[] getContexts();
}
