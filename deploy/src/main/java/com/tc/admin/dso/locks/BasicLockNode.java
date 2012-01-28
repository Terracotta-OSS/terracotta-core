/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

public abstract class BasicLockNode implements LockNode {
  public Object getValueAt(int column) {
    switch (column) {
      case 0:
        return getName();
      case 1:
        return Long.valueOf(getStats().getNumOfLockRequested());
      case 2:
        return Long.valueOf(getStats().getNumOfLockHopRequests());
      case 3:
        return Long.valueOf(getStats().getAvgNumberOfPendingRequests());
      case 4:
        return Long.valueOf(getStats().getAvgWaitTimeToAwardInMillis());
      case 5:
        return Long.valueOf(getStats().getAvgHeldTimeInMillis());
      case 6:
        return Long.valueOf(getStats().getAvgNestedLockDepth());
    }

    return null;
  }

  public String toString() {
    return getName();
  }
}
