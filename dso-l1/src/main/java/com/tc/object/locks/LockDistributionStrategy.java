/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.GroupID;

public interface LockDistributionStrategy {
  public GroupID getGroupIDFor(LockID lock);
}
