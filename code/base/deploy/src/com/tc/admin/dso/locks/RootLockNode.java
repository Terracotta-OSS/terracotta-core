/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.LockStats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class RootLockNode extends BasicLockNode {
  LockStatisticsMonitorMBean fLockStats;
  LockStats                  fStats = new LockStats();
  LockNode[]                 fChildren;

  RootLockNode(LockStatisticsMonitorMBean lockStats) {
    fLockStats = lockStats;
    init();
  }

  protected void init() {
    Collection<LockSpec> lockInfos = fLockStats.getLockSpecs();
    ArrayList<LockSpecNode> list = new ArrayList<LockSpecNode>();
    Iterator<LockSpec> iter = lockInfos.iterator();

    while (iter.hasNext()) {
      list.add(new LockSpecNode(iter.next()));
    }
    fChildren = list.toArray(new LockSpecNode[0]);
  }

  public String getName() {
    return "Locks Root";
  }

  public LockStats getStats() {
    return fStats;
  }

  public LockNode[] children() {
    return fChildren;
  }
}
