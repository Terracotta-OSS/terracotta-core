package com.tc.object.lockmanager.impl;

import com.tc.net.GroupID;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

public interface LockDistributionStrategy {
  GroupID getGroupIdForLock(String lockID);
}
