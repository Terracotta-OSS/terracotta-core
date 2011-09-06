/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import java.util.Set;

public class TestLocksRecallService implements LocksRecallService {
  public void recallLocks(Set<LockID> lockIds) {
    // do nothing
  }

  public void recallLocksInline(Set<LockID> lockIds) {
    // do nothing
  }
}
