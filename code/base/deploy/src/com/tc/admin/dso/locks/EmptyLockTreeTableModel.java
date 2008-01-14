/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.management.beans.LockStatisticsMonitorMBean;

public class EmptyLockTreeTableModel extends LockTreeTableModel {
  private static final LockNode[] EMPTY_LOCK_NODES = new LockNode[0];

  EmptyLockTreeTableModel(LockStatisticsMonitorMBean lockStats) {
    super(new RootLockNode(lockStats) {
      public LockNode[] children() {
        return EMPTY_LOCK_NODES;
      }
    });
  }
}
