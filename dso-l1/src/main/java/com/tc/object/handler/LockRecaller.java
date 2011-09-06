/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.object.locks.LockID;

import java.util.Set;

public interface LockRecaller {
  public void recallLocksInline(final Set<LockID> locks);
}
