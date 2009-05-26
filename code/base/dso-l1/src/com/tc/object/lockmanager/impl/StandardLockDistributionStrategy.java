/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.object.lockmanager.impl;

import com.tc.net.GroupID;

public class StandardLockDistributionStrategy implements LockDistributionStrategy {
  private final GroupID gid;

  public StandardLockDistributionStrategy(GroupID gid) {
    this.gid = gid;
  }

  public GroupID getGroupIdForLock(String lockID) {
    return gid;
  }
}
