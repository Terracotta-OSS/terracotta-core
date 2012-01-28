/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.management.lock.stats.LockStats;

public interface LockNode {
  String getName();

  LockStats getStats();

  Object getValueAt(int column);

  LockNode[] children();
}
