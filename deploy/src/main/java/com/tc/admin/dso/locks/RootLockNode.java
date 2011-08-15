/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.LockStats;
import com.tc.object.locks.LockID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class RootLockNode extends BasicLockNode {
  private LockStats  fStats = new LockStats();
  private LockNode[] fChildren;

  RootLockNode(Collection<LockSpec> lockInfos) {
    ArrayList<LockSpecNode> list = new ArrayList<LockSpecNode>();
    Iterator<LockSpec> iter = lockInfos.iterator();

    while (iter.hasNext()) {
      list.add(new LockSpecNode(iter.next()));
    }
    Collections.sort(list, new Comparator<LockSpecNode>() {
      public int compare(LockSpecNode o1, LockSpecNode o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
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
  
  public LockNode getLockNode(LockID lockID) {
    for(LockNode lockNode : children()) {
      LockSpecNode lockSpecNode = (LockSpecNode) lockNode;
      if(lockSpecNode.getSpec().getLockID().equals(lockID)) {
        return lockNode;
      }
    }
    return null;
  }
}
