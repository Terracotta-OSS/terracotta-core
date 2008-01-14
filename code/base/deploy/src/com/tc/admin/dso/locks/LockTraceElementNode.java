/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.management.lock.stats.LockStats;
import com.tc.management.lock.stats.LockTraceElement;

import java.util.ArrayList;
import java.util.Iterator;

public class LockTraceElementNode extends BasicLockNode {
  LockTraceElement fTraceElement;
  LockNode[]       fChildren;

  LockTraceElementNode(LockTraceElement element) {
    fTraceElement = element;

    ArrayList<LockTraceElementNode> list = new ArrayList<LockTraceElementNode>();
    Iterator<LockTraceElement> traceElementIter = element.children().iterator();
    while (traceElementIter.hasNext()) {
      list.add(new LockTraceElementNode(traceElementIter.next()));
    }
    fChildren = list.toArray(new LockTraceElementNode[0]);
  }

  public String getName() {
    return fTraceElement.getStackFrame().toString();
  }

  public LockStats getStats() {
    return fTraceElement.getStats();
  }

  public String getConfigText() {
    return fTraceElement.getConfigElement();
  }
  
  public LockNode[] children() {
    return fChildren;
  }
}
