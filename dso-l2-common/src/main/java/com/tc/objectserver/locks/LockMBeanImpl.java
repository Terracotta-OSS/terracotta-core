/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.object.locks.LockID;

import java.io.Serializable;
import java.util.Arrays;

public class LockMBeanImpl implements LockMBean, Serializable {
  private final LockID                  lockID;
  private final ServerLockContextBean[] contexts;

  public LockMBeanImpl(LockID lockID, ServerLockContextBean[] contexts) {
    this.lockID = lockID;
    this.contexts = contexts;
  }

  public ServerLockContextBean[] getContexts() {
    return contexts;
  }

  public LockID getLockID() {
    return lockID;
  }

  @Override
  public String toString() {
    return "LockMBeanImpl [contexts=" + Arrays.toString(contexts) + ", lockID=" + lockID + "]";
  }

}
